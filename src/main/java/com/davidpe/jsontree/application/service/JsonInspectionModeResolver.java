package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.out.LargePreviewSettingsStore;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonInspectionModeResolver {

  private final LargePreviewSettingsService settingsService;

  @Autowired
  public JsonInspectionModeResolver(LargePreviewSettingsService settingsService) {
    this.settingsService = settingsService;
  }

  public JsonInspectionModeResolver(LargePreviewProperties properties) {
    this(
        new LargePreviewSettingsService(
            new LargePreviewSettingsStore() {
              @Override
              public Optional<LargePreviewSettingsSnapshot> load() {
                return Optional.empty();
              }

              @Override
              public void save(LargePreviewSettingsSnapshot snapshot) {}
            },
            LargePreviewSettingsSnapshot.defaultsFrom(properties)));
  }

  public JsonInspectionMode resolve(JsonImportResult importResult) {
    return resolve(importResult.sizeBytes());
  }

  public JsonInspectionMode resolve(ImportedJsonFile historyEntry) {
    return resolve(historyEntry.sizeBytes());
  }

  public JsonInspectionMode resolve(long sizeBytes) {
    LargePreviewSettingsSnapshot settingsSnapshot = settingsService.current();
    return sizeBytes >= settingsSnapshot.largePreviewThresholdBytes()
        ? JsonInspectionMode.LARGE_PREVIEW
        : JsonInspectionMode.FULL;
  }
}
