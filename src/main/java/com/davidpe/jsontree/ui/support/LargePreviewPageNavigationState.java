package com.davidpe.jsontree.ui.support;

/**
 * Compact UI state for the large-preview page navigation strip shown in the main viewer.
 */
public record LargePreviewPageNavigationState(
    boolean visible,
    int currentPageNumber,
    int totalPages,
    boolean previousEnabled,
    boolean nextEnabled) {

  public static LargePreviewPageNavigationState hidden() {
    return new LargePreviewPageNavigationState(false, 0, 0, false, false);
  }
}
