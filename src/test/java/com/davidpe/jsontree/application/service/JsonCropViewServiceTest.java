package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonCropDocument;
import com.davidpe.jsontree.infrastructure.rendering.JacksonAsciiTreeFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonCropViewServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonCropViewService service =
      new JsonCropViewService(
          objectMapper,
          new JsonSemanticSearchPathResolverService(objectMapper),
          new JacksonAsciiTreeFormatter(objectMapper));

  @Test
  void buildsAnEphemeralCroppedJsonByMergingMatchedBranches() {
    JsonCropDocument cropDocument =
        service
            .buildFromQuery(
                """
                {
                  "user": {
                    "name": "David",
                    "address": {
                      "city": "Madrid",
                      "zip": "28001"
                    }
                  },
                  "games": [
                    { "opening": "Sicilian", "winner": "white" },
                    { "opening": "French", "winner": "black" }
                  ]
                }
                """,
                "Madrid|Sicilian|winner")
            .orElseThrow();

    assertEquals(
        "{\"user\":{\"address\":{\"city\":\"Madrid\"}},\"games\":[{\"opening\":\"Sicilian\",\"winner\":\"white\"},{\"winner\":\"black\"}]}",
        cropDocument.rawJson());
    assertTrue(cropDocument.asciiTreeDocument().content().contains("city: \"Madrid\""));
    assertTrue(cropDocument.asciiTreeDocument().content().contains("opening: \"Sicilian\""));
    assertTrue(cropDocument.asciiTreeDocument().content().contains("winner: \"black\""));
  }

  @Test
  void compactsArraysToOnlyTheMatchedElementsWhileKeepingOrder() {
    JsonCropDocument cropDocument =
        service
            .buildFromQuery(
                """
                {
                  "items": [
                    { "name": "alpha" },
                    { "name": "beta" },
                    { "name": "gamma" }
                  ]
                }
                """,
                "beta|gamma")
            .orElseThrow();

    assertEquals(
        "{\"items\":[{\"name\":\"beta\"},{\"name\":\"gamma\"}]}",
        cropDocument.rawJson());
  }

  @Test
  void returnsEmptyWhenTheActiveRegexDoesNotResolveToSemanticJsonBranches() {
    assertTrue(
        service
            .buildFromQuery(
                """
                {
                  "user": {
                    "name": "David"
                  }
                }
                """,
                "missing")
            .isEmpty());
  }
}
