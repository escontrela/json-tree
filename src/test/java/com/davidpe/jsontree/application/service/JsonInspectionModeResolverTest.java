package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.out.LargePreviewSettingsStore;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JsonInspectionModeResolverTest {

  @Test
  void resolvesFullModeAtOrBelowThreshold() {
    JsonInspectionModeResolver resolver = new JsonInspectionModeResolver(settingsService(1024));

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
    JsonInspectionModeResolver resolver = new JsonInspectionModeResolver(settingsService(1024));

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

  private LargePreviewSettingsService settingsService(long fullRenderMaxBytes) {
    return new LargePreviewSettingsService(
        new LargePreviewSettingsStore() {
          @Override
          public Optional<LargePreviewSettingsSnapshot> load() {
            return Optional.of(new LargePreviewSettingsSnapshot(fullRenderMaxBytes, 150 * 1024));
          }

          @Override
          public void save(LargePreviewSettingsSnapshot snapshot) {}
        },
        new LargePreviewSettingsSnapshot(fullRenderMaxBytes, 150 * 1024));
  }
}
