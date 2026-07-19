package com.davidpe.jsontree.ui.model;

import com.davidpe.jsontree.ui.support.ViewerTextRenderPlan;

/**
 * Presentation snapshot consumed by the secondary zoom window.
 */
public record ZoomViewerSnapshot(
    boolean renderable,
    String windowTitle,
    String modeLabel,
    String documentTitle,
    String documentMeta,
    ViewerTextRenderPlan renderPlan,
    String contentStyleClass,
    String emptyStateMessage) {

  public static ZoomViewerSnapshot renderable(
      String windowTitle,
      String modeLabel,
      String documentTitle,
      String documentMeta,
      ViewerTextRenderPlan renderPlan,
      String contentStyleClass) {
    return new ZoomViewerSnapshot(
        true,
        windowTitle,
        modeLabel,
        documentTitle,
        documentMeta,
        renderPlan,
        contentStyleClass,
        "");
  }

  public static ZoomViewerSnapshot empty(
      String windowTitle, String modeLabel, String documentTitle, String emptyStateMessage) {
    return new ZoomViewerSnapshot(
        false,
        windowTitle,
        modeLabel,
        documentTitle,
        "",
        null,
        "",
        emptyStateMessage == null ? "" : emptyStateMessage);
  }
}
