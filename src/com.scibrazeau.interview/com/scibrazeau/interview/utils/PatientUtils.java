package com.scibrazeau.interview.utils;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;

import java.text.SimpleDateFormat;

public class PatientUtils {
    public static String firstName(HumanName patient) {
        return patient == null ? null : patient.getGivenAsSingleString();
    }

    public static String firstName(Patient patient) {
        return patient == null ? null : firstName(CollectionUtils.first(patient.getName()));
    }

    public static String getNameWithBirthDate(Patient patient) {
        if (patient == null) {
            return null;
        }
        String date = patient.getBirthDate() != null ? " (" + new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthDate()) + ")" : "";
        return name(CollectionUtils.first(patient.getName())) + date;
    }

    private static String name(HumanName hn) {
        return hn == null ? null : (hn.getNameAsSingleString());
    }

}
