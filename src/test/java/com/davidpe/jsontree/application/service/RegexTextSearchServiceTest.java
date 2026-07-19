package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegexTextSearchServiceTest {

  private final RegexTextSearchService service = new RegexTextSearchService();

  @Test
  void executesRegexSearchOverArbitraryText() {
    var result = service.search("zoom:ascii", "name", "root\n└─ name: \"David\"\n└─ surname");

    assertTrue(result.successful());
    assertEquals(2, result.session().totalMatches());
    assertEquals(8, result.session().activeMatch().orElseThrow().startIndex());
  }

  @Test
  void reportsInvalidRegexWithoutThrowing() {
    var result = service.search("zoom:ascii", "[", "root\n└─ name");

    assertFalse(result.successful());
    assertTrue(result.errorMessage().toLowerCase().contains("unclosed"));
  }

  @Test
  void rejectsBlankQueriesAndMissingText() {
    assertFalse(service.search("zoom:ascii", "", "root").successful());
    assertFalse(service.search("zoom:ascii", "root", "").successful());
  }
}
