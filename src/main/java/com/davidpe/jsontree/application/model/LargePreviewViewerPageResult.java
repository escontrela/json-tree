package com.davidpe.jsontree.application.model;

public record LargePreviewViewerPageResult(
    JsonViewerLoadResult loadResult, boolean cacheHit, boolean waitedForAvailability) {

  public LargePreviewViewerPageResult {
    if (loadResult == null) {
      throw new IllegalArgumentException("Large-preview viewer load result is required.");
    }
  }
}
