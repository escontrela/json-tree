package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.service.JsonInspectionModeResolver;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LargePreviewIndicatorResolverTest {

  @Test
  void showsWarningForLargeCurrentView() {
    LargePreviewIndicatorResolver resolver = resolver(1024);

    assertTrue(resolver.showForCurrentView(largeViewResult()));
    assertFalse(resolver.showForCurrentView(smallViewResult()));
  }

  @Test
  void showsWarningForLargeHistoryEntries() {
    LargePreviewIndicatorResolver resolver = resolver(1024);

    assertTrue(
        resolver.showForHistoryEntry(
            new ImportedJsonFile(
                "large.json",
                "large.json",
                Instant.parse("2026-07-04T10:00:00Z"),
                4096,
                200,
                true,
                false)));
    assertFalse(
        resolver.showForHistoryEntry(
            new ImportedJsonFile(
                "small.json",
                "small.json",
                Instant.parse("2026-07-04T10:00:00Z"),
                128,
                20,
                true,
                false)));
  }

  private LargePreviewIndicatorResolver resolver(long fullRenderMaxBytes) {
    LargePreviewProperties properties = new LargePreviewProperties();
    properties.setFullRenderMaxBytes(fullRenderMaxBytes);
    return new LargePreviewIndicatorResolver(new JsonInspectionModeResolver(properties));
  }

  private JsonViewerLoadResult largeViewResult() {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/large.json"),
            "large.json",
            4096,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
        new AsciiTreeDocument("root", "root\n└─ large: true", 2),
        null,
        JsonInspectionMode.LARGE_PREVIEW,
        JsonViewerCapabilities.largePreview(),
        null);
  }

  private JsonViewerLoadResult smallViewResult() {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/small.json"),
            "small.json",
            128,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
        new AsciiTreeDocument("root", "root\n└─ small: true", 2),
        null,
        JsonInspectionMode.FULL,
        JsonViewerCapabilities.full(),
        null);
  }
}
