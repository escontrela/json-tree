package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonBreadcrumbPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonBreadcrumbModelServiceTest {

  private final JsonBreadcrumbModelService service =
      new JsonBreadcrumbModelService(
          new ObjectMapper(),
          new RawJsonPresentationService(new ObjectMapper(), new BestEffortJsonPrettyPrinter()));

  @Test
  void buildsBreadcrumbAnchorsWithNamedPropertiesAndRealArrayIndexes() {
    JsonBreadcrumbModel model =
        service.buildFromRawJson(
            """
            {
              "user": {
                "address": {
                  "city": "Madrid"
                }
              },
              "games": [
                {
                  "opening_name": "Sicilian"
                },
                {
                  "opening_name": "French"
                }
              ]
            }
            """);

    assertTrue(model.available());
    assertTrue(
        model.anchors().stream()
            .map(anchor -> anchor.path().displayLabel())
            .anyMatch("root / user / address / city"::equals));
    assertTrue(
        model.anchors().stream()
            .map(anchor -> anchor.path().displayLabel())
            .anyMatch("root / games / [1] / opening_name"::equals));
  }

  @Test
  void preservesRootForScalarDocuments() {
    JsonBreadcrumbModel model = service.buildFromRawJson("\"hello\"");

    assertEquals(1, model.anchors().size());
    assertEquals(JsonBreadcrumbPath.root(), model.anchors().getFirst().path());
  }
}
