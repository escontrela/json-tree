package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.DocumentFormat;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ZoomActionAvailabilityResolverTest {

  private final ZoomActionAvailabilityResolver resolver = new ZoomActionAvailabilityResolver();

  @Test
  void disablesZoomWhenNoRenderableDocumentExists() {
    assertFalse(resolver.shouldEnable(null));
    assertFalse(
        resolver.shouldEnable(
                new JsonViewerLoadResult(
                new JsonImportResult(
                    Path.of("/tmp/sample.json"),
                    "sample.json",
                    0L,
                    true,
                    true,
                    true,
                    JsonDocumentSourceKind.LOCAL_FILE),
                new JsonValidationResult(
                    JsonValidationStatus.INVALID, "Invalid JSON", null, null),
                null,
                null,
                JsonInspectionMode.FULL,
                JsonViewerCapabilities.full(),
                null)));
  }

  @Test
  void enablesZoomForValidFullAndLargePreviewDocuments() {
    assertTrue(resolver.shouldEnable(renderableResult(JsonInspectionMode.FULL)));
    assertTrue(resolver.shouldEnable(renderableResult(JsonInspectionMode.LARGE_PREVIEW)));
    assertTrue(resolver.shouldEnable(markdownResult()));
  }

  private JsonViewerLoadResult renderableResult(JsonInspectionMode mode) {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/sample.json"),
            "sample.json",
            1024L,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON", null, null),
        new AsciiTreeDocument("root", "root\n└─ id: 1", 2),
        null,
        mode,
        mode == JsonInspectionMode.LARGE_PREVIEW
            ? JsonViewerCapabilities.largePreview()
            : JsonViewerCapabilities.full(),
        null);
  }

  private JsonViewerLoadResult markdownResult() {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/readme.md"),
            "readme.md",
            1024L,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE,
            DocumentFormat.MARKDOWN),
        new JsonValidationResult(JsonValidationStatus.VALID, "Markdown ready", null, null),
        new AsciiTreeDocument("readme.md", "# Heading\n\ncontent", 3),
        null,
        JsonInspectionMode.FULL,
        new JsonViewerCapabilities(true, true, true),
        null);
  }
}
