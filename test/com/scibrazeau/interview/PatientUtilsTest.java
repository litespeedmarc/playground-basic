package com.scibrazeau.interview;

import com.scibrazeau.interview.utils.CollectionUtils;
import com.scibrazeau.interview.utils.PatientUtils;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class PatientUtilsTest {

    @Test
    public void testFirstNamePatient() {
        Assert.assertNull(PatientUtils.firstName((Patient) null));
        Assert.assertEquals("Marc", PatientUtils.firstName(createTestPatient(new String[] {"Marc"}, null)));
        Assert.assertEquals("Marc Roger", PatientUtils.firstName(createTestPatient(new String[] {"Marc", "Roger"}, null)));
        Assert.assertEquals("Marc Roger", PatientUtils.firstName(createTestPatient(new String[] {"Marc", "Roger"}, "Brazeau")));
        Assert.assertEquals("", PatientUtils.firstName(createTestPatient(new String[] {""}, null)));
        Assert.assertEquals("Marc", PatientUtils.firstName(createTestPatient(new String[]{null, "Marc"}, null)));
        Assert.assertEquals("", PatientUtils.firstName(createTestPatient(new String[] {}, null)));
    }


    @Test
    public void testFirstNameHuman() {
        Assert.assertNull(PatientUtils.firstName((HumanName) null));
        Assert.assertEquals("Marc", PatientUtils.firstName(createTestHumanName(new String[] {"Marc"}, null)));
        Assert.assertEquals("Marc Roger", PatientUtils.firstName(createTestHumanName(new String[] {"Marc", "Roger"}, null)));
        Assert.assertEquals("Marc Roger", PatientUtils.firstName(createTestHumanName(new String[] {"Marc", "Roger"}, "Brazeau")));
        Assert.assertEquals("", PatientUtils.firstName(createTestHumanName(new String[] {""}, null)));
        Assert.assertEquals("Marc", PatientUtils.firstName(createTestHumanName(new String[]{null, "Marc"}, null)));
        Assert.assertEquals("", PatientUtils.firstName(createTestHumanName(new String[] {}, null)));
    }

    @Test
    public void testGetNameWithBirthDate() throws ParseException {
        Assert.assertNull(PatientUtils.getNameWithBirthDate(null));
        Assert.assertEquals("Marc Brazeau (1973-09-19)", PatientUtils.getNameWithBirthDate(createTestPatient("Marc", "Brazeau", "1973/09/19")));
        Assert.assertEquals("Marc Brazeau", PatientUtils.getNameWithBirthDate(createTestPatient("Marc", "Brazeau", null)));
    }

    public static String name(HumanName patient) {
        return patient == null ? null : (patient.getNameAsSingleString());
    }


    private Patient createTestPatient(String given, String surname, String birthDate) throws ParseException {
        Patient p = createTestPatient(new String[]{given}, surname);
        if (birthDate != null) {
            p.setBirthDate(new SimpleDateFormat("yyyy/MM/dd").parse(birthDate));
        }
        return p;
    }

    private Patient createTestPatient(String[] givens, String surname) {
        Patient p = new Patient();
        p.addName(createTestHumanName(givens, surname));
        return p;
    }

    private HumanName createTestHumanName(String[] givens, String surname) {
        HumanName hn = new HumanName();
        Arrays.stream(givens).forEach(hn::addGiven);
        hn.setFamily(surname);
        return hn;
    }

}
