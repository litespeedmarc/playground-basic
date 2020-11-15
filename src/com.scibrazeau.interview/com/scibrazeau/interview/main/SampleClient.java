package com.scibrazeau.interview.main;

import com.scibrazeau.interview.utils.SampleNamePrinter;

public class SampleClient {

    public static void main(String[] theArgs) {
        SampleNamePrinter sc = new SampleNamePrinter();
        sc.addName("Smith");
        sc.printNames();
    }

}
