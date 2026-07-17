package com.davidpe.jsontree.application.model;

public record LargePreviewPageLoadResult(
    LargePreviewPagedSession session,
    LargePreviewPageContent page,
    boolean cacheHit,
    boolean waitedForAvailability) {

  public LargePreviewPageLoadResult {
    if (session == null) {
      throw new IllegalArgumentException("Large-preview session is required.");
    }
    if (page == null) {
      throw new IllegalArgumentException("Large-preview page content is required.");
    }
  }
}
