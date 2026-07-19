package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonStructureDocumentServiceTest {

  private final JsonStructureDocumentService service =
      new JsonStructureDocumentService(new ObjectMapper());

  @Test
  void rendersNestedObjectStructureWithoutRealValues() {
    String content =
        service
            .buildFromRawJson(
                """
                {
                  "user": {
                    "address": {
                      "city": "Madrid"
                    }
                  }
                }
                """)
            .content();

    assertTrue(content.contains("root"));
    assertTrue(content.contains("user"));
    assertTrue(content.contains("address"));
    assertTrue(content.contains("city"));
    assertFalse(content.contains("Madrid"));
    assertFalse(content.contains(":"));
  }

  @Test
  void preservesOriginalPropertyNamesAndShowsEmptyContainersByNameOnly() {
    String content =
        service
            .buildFromRawJson(
                """
                {
                  "api-response": {},
                  "snake_case": []
                }
                """)
            .content();

    assertTrue(content.contains("api-response"));
    assertTrue(content.contains("snake_case []"));
    assertFalse(content.contains("\"api-response\""));
  }

  @Test
  void rendersTopLevelArraysWithRepresentativeNode() {
    String content =
        service
            .buildFromRawJson(
                """
                [
                  {
                    "opening_name": "Sicilian"
                  }
                ]
                """)
            .content();

    assertTrue(content.startsWith("root []"));
    assertTrue(content.contains("[0]"));
    assertTrue(content.contains("opening_name"));
    assertFalse(content.contains("Sicilian"));
  }

  @Test
  void rendersScalarArraysAsPlaceholderOnly() {
    String content =
        service
            .buildFromRawJson(
                """
                {
                  "ids": [1, 2, 3]
                }
                """)
            .content();

    assertEquals("root\n└─ ids []\n   └─ [0]", content);
  }
}
