package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.service.BestEffortJsonPrettyPrinter;
import com.davidpe.jsontree.application.service.JsonBreadcrumbModelService;
import com.davidpe.jsontree.application.service.RawJsonPresentationService;
import com.davidpe.jsontree.ui.model.BreadcrumbViewerMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonBreadcrumbViewportResolverTest {

  private final JsonBreadcrumbViewportResolver resolver = new JsonBreadcrumbViewportResolver();
  private final JsonBreadcrumbModelService modelService =
      new JsonBreadcrumbModelService(
          new ObjectMapper(),
          new RawJsonPresentationService(new ObjectMapper(), new BestEffortJsonPrettyPrinter()));

  @Test
  void resolvesTopOfDocumentToRootAndKeepsNearestValidPathForAscii() {
    JsonBreadcrumbModel model =
        modelService.buildFromRawJson(
            """
            {
              "app": {
                "window": {
                  "width": 1280,
                  "height": 720
                }
              }
            }
            """);

    assertEquals(
        "root",
        resolver.resolve(model, BreadcrumbViewerMode.ASCII_TREE, 0).orElseThrow().displayLabel());
    assertEquals(
        "root / app / window / width",
        resolver.resolve(model, BreadcrumbViewerMode.ASCII_TREE, 3).orElseThrow().displayLabel());
    assertEquals(
        "root / app / window / height",
        resolver.resolve(model, BreadcrumbViewerMode.ASCII_TREE, 99).orElseThrow().displayLabel());
  }

  @Test
  void resolvesRawModeToNearestEnclosingNamedPathWithRealArrayIndexes() {
    JsonBreadcrumbModel model =
        modelService.buildFromRawJson(
            """
            {
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

    int targetRawLine =
        model.anchors().stream()
            .filter(anchor -> "root / games / [1] / opening_name".equals(anchor.path().displayLabel()))
            .findFirst()
            .orElseThrow()
            .rawDisplayLineIndex();

    assertEquals(
        "root / games / [1] / opening_name",
        resolver
            .resolve(model, BreadcrumbViewerMode.RAW_JSON, targetRawLine)
            .orElseThrow()
            .displayLabel());
  }

  @Test
  void reportsUnavailableForMissingModels() {
    assertTrue(resolver.resolve(JsonBreadcrumbModel.unavailable(), BreadcrumbViewerMode.ASCII_TREE, 0).isEmpty());
  }
}
