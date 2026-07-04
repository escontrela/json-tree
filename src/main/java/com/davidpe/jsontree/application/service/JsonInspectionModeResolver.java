package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import org.springframework.stereotype.Service;

@Service
public class JsonInspectionModeResolver {

  private final LargePreviewProperties properties;

  public JsonInspectionModeResolver(LargePreviewProperties properties) {
    this.properties = properties;
  }

  public JsonInspectionMode resolve(JsonImportResult importResult) {
    return resolve(importResult.sizeBytes());
  }

  public JsonInspectionMode resolve(ImportedJsonFile historyEntry) {
    return resolve(historyEntry.sizeBytes());
  }

  public JsonInspectionMode resolve(long sizeBytes) {
    return sizeBytes > properties.getFullRenderMaxBytes()
        ? JsonInspectionMode.LARGE_PREVIEW
        : JsonInspectionMode.FULL;
  }
}
