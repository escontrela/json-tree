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

    assertEquals("root\n└─ user\n   └─ address\n      └─ city", content);
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

    assertEquals("root\n├─ api-response\n└─ snake_case []", content);
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

    assertEquals("root []\n└─ [0]\n   └─ opening_name", content);
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

  @Test
  void rendersTopLevelObjectFieldsDirectlyUnderRoot() {
    String content =
        service
            .buildFromRawJson(
                """
                {
                  "app": {},
                  "theme": "dark"
                }
                """)
            .content();

    assertEquals("root\n├─ app\n└─ theme", content);
  }
}
