package com.scibrazeau.interview.main;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.scibrazeau.interview.utils.PatientUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

public class SampleClient {
    private static Log kLogger = LogFactory.getLog(SampleClient.class);

    public static void main(String[] theArgs) {

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        // Search for Patient resources
        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value("SMITH"))
                .count(100)
                .sort(new SortSpec("given"))
                .returnBundle(Bundle.class)
                .execute();

        // Unclear, only first 20 or all?  Doing All
        while (true) {
            response.getEntry().stream()
                    .map(bec -> (Patient)bec.getResource())
                    .forEach(SampleClient::print);
            if (response.getLink(IBaseBundle.LINK_NEXT) != null) {
                response = client
                        .loadPage()
                        .next(response)
                        .execute();
            } else {
                break;
            }
        }
    }

    private static void print(Patient patient) {
        kLogger.info(PatientUtils.getNameWithBirthDate(patient));
    }

}
