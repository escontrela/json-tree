package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.RawJsonPresentation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RawJsonPresentationServiceTest {

  private final RawJsonPresentationService service =
      new RawJsonPresentationService(new ObjectMapper());

  @Test
  void prettyPrintsMinifiedJsonIntoReadableMultilineContent() {
    String rawJson = "{\"name\":\"David\",\"roles\":[\"admin\"]}";
    RawJsonPresentation presentation = service.present(rawJson);

    assertTrue(presentation.content().contains(System.lineSeparator()) || presentation.content().contains("\n"));
    assertTrue(presentation.content().contains("  \"name\" : \"David\""));
    assertEquals(rawJson.length() + 1, presentation.sourceToDisplayBoundaries().length);
  }

  @Test
  void preservesStringWhitespaceWhenBuildingDisplayBoundaryMap() {
    String rawJson = "{\"message\":\"hello world\",\"active\":true}";

    RawJsonPresentation presentation = service.present(rawJson);
    int start = rawJson.indexOf("hello world");
    int end = start + "hello world".length();
    String highlighted = presentation.content().substring(
        presentation.sourceToDisplayBoundaries()[start],
        presentation.sourceToDisplayBoundaries()[end]);

    assertEquals("hello world", highlighted);
  }
}
