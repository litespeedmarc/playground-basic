package com.scibrazeau.interview.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

/***
 *  A utility that connects to a fhir server, and that prints names of patients that match a given list of names.
 */
public class SampleNamePrinter {
    private static final Log kLogger = LogFactory.getLog(SampleNamePrinter.class);

    private final IGenericClient mClient;
    private final List<String> mNames = new ArrayList<>();

    private BiConsumer<HumanName, Date> mNamePrinter = (hn, bd) -> {
        String date = bd != null ? " (" + new SimpleDateFormat("yyyy-MM-dd").format(bd) + ")" : "";
        kLogger.info(hn.getNameAsSingleString() + date);
    };

    private boolean mCache = true;
    private int mMaxPages; // number of pages to retrieve.  <= 0, means retrieve all.

    // for testing
    /* package */ void setNamePrinter(BiConsumer<HumanName, Date> printer) {
        mNamePrinter = printer;
    }

    /* package */ void disableCache(boolean disable) {
        mCache = !disable;
    }

    public SampleNamePrinter() {
        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
        mClient = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
    }

    public void printNames() {
        if (mNames.isEmpty()) {
            throw new IllegalArgumentException("Please add some names to search on first");
        }

        mNames.forEach(this::printNames);
    }

    private void printNames(String name) {
        // Search for Patient resources
        IQuery<Bundle> query = mClient
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value(name))
                .count(100)
                .sort(new SortSpec("given"))
                .returnBundle(Bundle.class);

        if (!mCache) {
            query.cacheControl(new CacheControlDirective().setNoCache(true));
        }

        Bundle response = query.execute();

        // Unclear, only first 20 or all?  Doing All
        int pageAt = 0;
        while (true) {
            response.getEntry().stream()
                    .map(bec -> (Patient) bec.getResource())
                    .forEach(p -> this.print(name, p));
            pageAt++;
            if ((mMaxPages <= 0 || pageAt < mMaxPages) && response.getLink(IBaseBundle.LINK_NEXT) != null) {
                response = mClient
                        .loadPage()
                        .next(response)
                        .execute();
            } else {
                break;
            }
        }
    }

    public void addNames(Collection<String> names) {
        mNames.addAll(names);
    }

    public void addName(String name) {
        mNames.add(name);
    }

    private void print(String nameSought, Patient patient) {
        // print the names we searched on.
        // TODO: This should probably be accent insensitive too
        HumanName name = patient.getName().stream().filter(hn -> hn.getFamily().toUpperCase().contains(nameSought.toUpperCase())).findFirst().orElse(null);
        mNamePrinter.accept(name, patient.getBirthDate());
    }

    public void addClientInterceptor(IClientInterceptor iClientInterceptor) {
        mClient.registerInterceptor(iClientInterceptor);
    }


    public void setMaxPages(int maxPages) {
        mMaxPages = maxPages;
    }
}
