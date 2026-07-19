package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RawJsonPresentationLargePreviewTest {

  private final RawJsonPresentationService service =
      new RawJsonPresentationService(new ObjectMapper(), new BestEffortJsonPrettyPrinter());

  @Test
  void keepsLargePreviewChunksUnmodifiedByDefault() {
    String rawChunk = "{\"id\":1,\"name\":\"David\"}";

    var presentation = service.presentLargePreviewChunk(rawChunk, false);

    assertEquals(rawChunk, presentation.content());
    assertEquals(0, presentation.sourceToDisplayBoundaries()[0]);
    assertEquals(rawChunk.length(), presentation.sourceToDisplayBoundaries()[rawChunk.length()]);
  }

  @Test
  void prettyPrintsLargePreviewChunksOnlyWhenExplicitlyEnabled() {
    String rawChunk = "{\"id\":1,\"name\":\"David\"}";

    var presentation = service.presentLargePreviewChunk(rawChunk, true);

    assertTrue(presentation.content().contains("\n"));
    assertFalse(presentation.content().equals(rawChunk));
  }
}
