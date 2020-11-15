package com.scibrazeau.interview.utils;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SampleNamePrinterIntegrationTest {
    private static final Log kLogger = LogFactory.getLog(SampleNamePrinterIntegrationTest.class);
    private static final int kReads = 3;

    private static class ResponseInfo {
        String name;
        List<Long> responseTimes = new ArrayList<>();
        List<AtomicInteger> namesRetrieved = new ArrayList<>();
    }

    /**
     * Test that when running sample name printer with a list of names:
     * <p>
     * 1) we searched on the names provided
     * 2) the search results more or less make sense.  Note that because the server is random, we can't
     * make assumptions and verify the exact results returned.  Here is what we can test:
     * a) not all the same # of results returned
     * b) given name included in results
     * c) at least some results
     * 3) if we turn caching off, then on average the third search time should be greater than the 2nd search time
     */
    @Test
    public void testNamePrinter() throws IOException {
        SampleNamePrinter sc = new SampleNamePrinter();

        Map<String, ResponseInfo> responses = new HashMap<>();
        ResponseInfo[] activeResponseInfo = new ResponseInfo[1];

        // read a bunch of names.
        List<String> names =
                IOUtils.readLines(SampleNamePrinterIntegrationTest.class.getResourceAsStream("SampleNamePrinterTest.surnames.txt"), "UTF-8")
                        .stream()
                        // remove garbage entries that dev may have accidentally left in input files (empty lines or trailing/leading spaces)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(n -> n.length() > 0)
                        .collect(Collectors.toList());

        sc.addNames(names);
        sc.setMaxPages(1);

        sc.setNamePrinter((hn, bd) -> {
            // don't clutter sysout, we just count the number of results printed
            List<AtomicInteger> nameCounts = activeResponseInfo[0].namesRetrieved;
            nameCounts.get(nameCounts.size() - 1).incrementAndGet();

            // also make sure patient's given name contains the name sought.
            String nameSought = activeResponseInfo[0].name.toUpperCase();
            String namePrinted = hn.getFamily().toUpperCase();
            Assert.assertTrue("Expected family name to contain " + nameSought + ", but it did not. It was: " + namePrinted, namePrinted.contains(nameSought));
        });

        sc.addClientInterceptor(new IClientInterceptor() {
            @Override
            public void interceptRequest(IHttpRequest iHttpRequest) {
                String uri = iHttpRequest.getUri();
                if (uri.contains("?_getpages")) {
                    Assert.fail("When only requesting 1 page (as is the case for this test), name printer should not make any subsequent calls to retrieve additional pages");
                }
                int idx = uri.indexOf("/Patient?family=");
                if (idx >= 0) {
                    uri = uri.substring(idx + "/Patient?".length());
                    List<NameValuePair> params = URLEncodedUtils.parse(uri, Charset.defaultCharset());
                    NameValuePair family = params.get(0);
                    String key = family.getName();
                    String value = family.getValue();
                    Assert.assertEquals("family", key);
                    Assert.assertTrue(value.length() > 0);
                    activeResponseInfo[0] = responses.get(value);
                    if (activeResponseInfo[0] == null) {
                        activeResponseInfo[0] = new ResponseInfo();
                        responses.put(value, activeResponseInfo[0]);
                        activeResponseInfo[0].name = value;
                    }
                } else {
                    activeResponseInfo[0] = null;
                }
            }

            @Override
            public void interceptResponse(IHttpResponse iHttpResponse) {
                if (activeResponseInfo[0] != null) {
                    long time = iHttpResponse.getRequestStopWatch().getMillis();
                    activeResponseInfo[0].responseTimes.add(time);
                    activeResponseInfo[0].namesRetrieved.add(new AtomicInteger());
                    kLogger.info("Responded to " + activeResponseInfo[0].name + " (#" + activeResponseInfo[0].responseTimes.size() + ") in " + time + "ms");
                }
            }
        });

        for (int i = 0; i < kReads; i++) {
            sc.disableCache(i == 2);
            sc.printNames();
        }

        // every name should have been searched.
        Assert.assertEquals("Should have searched for all names requested", names.size(), responses.size());
        names.removeAll(responses.keySet());
        Assert.assertTrue("Searched for the same number of names, but apparently not the same names!  Names missed == [" + String.join(", ", names) + "]", names.isEmpty());

        int greaterCount = 0;
        int min = Integer.MAX_VALUE;
        int max = 0;

        for (ResponseInfo ri : responses.values()) {
            Assert.assertEquals(ri.name, kReads, ri.responseTimes.size());
            Assert.assertEquals(ri.name, kReads, ri.namesRetrieved.size());
            Assert.assertTrue("Should have retrieved the same number of patients in all cases for " + ri.name, ri.namesRetrieved.stream().allMatch(ai -> ai.get() == ri.namesRetrieved.get(0).get()));
            String times = Iterables.toString(ri.responseTimes);
            if (ri.responseTimes.get(2) > ri.responseTimes.get(1)) {
                greaterCount++;
            }
            int countNames = ri.namesRetrieved.get(0).get();
            if (countNames > 0) {
                min = Math.min(min, countNames);
            }
            max = Math.max(max, countNames);
            kLogger.info("GET for " + ri.name + " retrieved " + ri.namesRetrieved.get(0).get() + " patients (" + kReads + " times), with respective times of " + times + " ms");
        }

        Assert.assertTrue("Very suspicious that when results are retrieved, we always retrieve the same # of records (" + max + ")", min != max);
        Assert.assertTrue("Most requests should be slower the 3rd time around, because we turned off caching.  Only " + greaterCount + " requests were slower", greaterCount > names.size() * 100 / 75);
    }

}
