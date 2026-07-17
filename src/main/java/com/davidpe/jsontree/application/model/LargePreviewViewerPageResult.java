package com.davidpe.jsontree.application.model;

/**
 * Result of swapping the active page inside the main viewer for a paged large-preview session.
 *
 * <p>The wrapper keeps the refreshed viewer state together with cache-hit and wait metadata so the
 * UI can decide whether to show loading affordances or move immediately.
 */
public record LargePreviewViewerPageResult(
    JsonViewerLoadResult loadResult, boolean cacheHit, boolean waitedForAvailability) {

  public LargePreviewViewerPageResult {
    if (loadResult == null) {
      throw new IllegalArgumentException("Large-preview viewer load result is required.");
    }
  }
}
