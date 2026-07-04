package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JsonInspectionModeResolverTest {

  @Test
  void resolvesFullModeAtOrBelowThreshold() {
    JsonInspectionModeResolver resolver = new JsonInspectionModeResolver(properties(1024));

    JsonInspectionMode mode =
        resolver.resolve(
            new JsonImportResult(
                Path.of("/tmp/small.json"),
                "small.json",
                1024,
                true,
                true,
                true,
                JsonDocumentSourceKind.LOCAL_FILE));

    assertEquals(JsonInspectionMode.FULL, mode);
  }

  @Test
  void resolvesLargePreviewModeAboveThresholdForHistoryEntries() {
    JsonInspectionModeResolver resolver = new JsonInspectionModeResolver(properties(1024));

    JsonInspectionMode mode =
        resolver.resolve(
            new ImportedJsonFile(
                "stored.json",
                "stored.json",
                Instant.parse("2026-07-04T10:00:00Z"),
                1025,
                12,
                true,
                false));

    assertEquals(JsonInspectionMode.LARGE_PREVIEW, mode);
  }

  private LargePreviewProperties properties(long fullRenderMaxBytes) {
    LargePreviewProperties properties = new LargePreviewProperties();
    properties.setFullRenderMaxBytes(fullRenderMaxBytes);
    return properties;
  }
}
