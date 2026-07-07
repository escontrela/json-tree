package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BestEffortJsonPrettyPrinterTest {

  private final BestEffortJsonPrettyPrinter printer = new BestEffortJsonPrettyPrinter();

  @Test
  void preservesEscapedQuotesAndBackslashesInsideStrings() {
    String rawJson = "{\"message\":\"He said \\\"hi\\\" and kept C:\\\\temp\",\"flag\":true";

    String formatted = printer.prettyPrint(rawJson);

    assertTrue(formatted.contains("He said \\\"hi\\\" and kept C:\\\\temp"));
    assertTrue(formatted.contains("\"flag\" : true"));
  }

  @Test
  void keepsEmptyContainersCompactWhileFormattingStructure() {
    String rawJson = "{\"object\":{},\"array\":[]}";

    String formatted = printer.prettyPrint(rawJson);

    assertEquals("{\n  \"object\" : {},\n  \"array\" : []\n}", formatted);
  }
}
