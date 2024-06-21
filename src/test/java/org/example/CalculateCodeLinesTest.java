package org.example;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

class CalculateCodeLinesTest {
  private final CalculateCodeLines classUnderTest = new CalculateCodeLines();

  @Test
  void testNormal() {
    assert (1 == classUnderTest.calculate("import;")) : "incorrect calculation";
  }

  @Test
  void testMultiInLine() {
    assert (3 == classUnderTest.calculate("""
        import java.lang.String;
        import java.lang.String;
        System/*.out.println("Das hat die \\"Kode\\" besucht");*/
        """)) : "incorrect calculation";
  }

  @Test
  void testMultiInLineQuoterAfterQuaterMark() {
    assert (3 == classUnderTest.calculate("""
        import java.lang.String;
        import java.lang.String;
        System/*.out*/.println("Das hat die \\"Kode\\" besucht");
        """)) : "incorrect calculation";
  }

  @Test
  void testQuotes() {
    assert (3 == classUnderTest.calculate("""
        import java.lang.String;
        System/*.out*/.println("Das ist die \\"Kode\\"");
        System/*.out*/.println("Das ist die \\"Kode 2\\"");
        """)) : "incorrect calculation";
  }

  @Test
  void testQuotesFromFile() throws IOException {
    try (InputStream stream = CalculateCodeLinesTest.class.getResourceAsStream("/testQuotesFromFile.txt")) {
      assert (stream != null) : "Can't read file";
      String lanes = new String(stream.readAllBytes());
      assert (2 == classUnderTest.calculate(lanes)) : "incorrect calc";
    }
  }

  @Test
  void testMultilineQuotes() {
    assert (3 == classUnderTest.calculate("""
        import java.lang.String;
        System.out.println("Das
        ist die \\"Kode\\"");
        """)) : "incorrect calculation";
  }

  @Test
  void testMultilineQuotesFromFile() throws IOException {
    try (InputStream stream = CalculateCodeLinesTest.class.getResourceAsStream("/testMultilineQuotesFromFile.txt")) {
      assert (stream != null) : "Can't read file";
      String lanes = new String(stream.readAllBytes());
      assert (4 == classUnderTest.calculate(lanes)) : "incorrect calc";
    }
  }

  @Test
  void testComplexExampleFromFile() throws IOException {
    try (InputStream stream = CalculateCodeLinesTest.class.getResourceAsStream("/testComplexExample.txt")) {
      assert (stream != null) : "Can't read file";
      String lanes = new String(stream.readAllBytes());
      assert (20 == classUnderTest.calculate(lanes)) : "incorrect calc";
    }
  }
}