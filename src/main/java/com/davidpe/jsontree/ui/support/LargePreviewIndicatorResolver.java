package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.service.JsonInspectionModeResolver;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import org.springframework.stereotype.Component;

@Component
public class LargePreviewIndicatorResolver {

  private final JsonInspectionModeResolver inspectionModeResolver;

  public LargePreviewIndicatorResolver(JsonInspectionModeResolver inspectionModeResolver) {
    this.inspectionModeResolver = inspectionModeResolver;
  }

  public boolean showForCurrentView(JsonViewerLoadResult result) {
    return result != null && result.usesLargePreview();
  }

  public boolean showForHistoryEntry(ImportedJsonFile entry) {
    return inspectionModeResolver.resolve(entry) == JsonInspectionMode.LARGE_PREVIEW;
  }
}
