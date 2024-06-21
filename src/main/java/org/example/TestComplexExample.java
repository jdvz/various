package org.example;

import java.lang.String;

/**
 * this is example, and this is in java folder only to have a possibility to run it.
 */
public class TestComplexExample {
  public static void main(String ... args) {
    System.out.println("""
    Das ist die \"Kode\" I wrote before
    """);

    /*
    test("example")
    */
    // no code here
    TestComplexExample.test("example 2"); // no code later
    System.out.println("""
    Das ist die "Kode" /* comment doesn't matter */
    I wrote before
    """);

    System.exit(0);
  }

  public static void test(String example) {
    System.out.println(
        ""
    );
  }
}