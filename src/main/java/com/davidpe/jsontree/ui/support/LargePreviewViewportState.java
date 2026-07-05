package com.davidpe.jsontree.ui.support;

/**
 * Shared large-preview viewport state used by the viewer, outline, and page controls so they all
 * point to the same oversized-document position.
 */
public record LargePreviewViewportState(
    boolean active, int currentPageIndex, int totalPages, double globalScrollValue) {

  public LargePreviewViewportState {
    if (currentPageIndex < 0) {
      throw new IllegalArgumentException("Large-preview viewport page index must be non-negative.");
    }
    if (totalPages < 0) {
      throw new IllegalArgumentException("Large-preview viewport total pages must be non-negative.");
    }
    if (active && totalPages <= 0) {
      throw new IllegalArgumentException("Active large-preview viewport state requires total pages.");
    }
    if (active && currentPageIndex >= totalPages) {
      throw new IllegalArgumentException(
          "Active large-preview viewport page index must stay within total pages.");
    }
    globalScrollValue = clamp(globalScrollValue);
  }

  public static LargePreviewViewportState inactive() {
    return new LargePreviewViewportState(false, 0, 0, 0.0);
  }

  public int currentPageNumber() {
    return active ? currentPageIndex + 1 : 0;
  }

  public boolean previousEnabled() {
    return active && currentPageIndex > 0;
  }

  public boolean nextEnabled() {
    return active && currentPageIndex + 1 < totalPages;
  }

  private static double clamp(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return 0.0;
    }
    return Math.max(0.0, Math.min(1.0, value));
  }
}
