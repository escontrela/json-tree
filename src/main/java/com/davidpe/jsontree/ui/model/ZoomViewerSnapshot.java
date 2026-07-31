package com.davidpe.jsontree.ui.model;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlan;

/** Presentation snapshot consumed by the secondary zoom window. */
public record ZoomViewerSnapshot(
    boolean renderable,
    boolean largePreview,
    String windowTitle,
    String modeLabel,
    String documentTitle,
    String documentMeta,
    String sourceRawText,
    ViewerTextRenderPlan renderPlan,
    String contentStyleClass,
    ViewerPresentationMode presentationMode,
    JsonBreadcrumbModel breadcrumbModel,
    JsonOutlineModel outlineModel,
    String emptyStateMessage) {

  public static ZoomViewerSnapshot renderable(
      boolean largePreview,
      String windowTitle,
      String modeLabel,
      String documentTitle,
      String documentMeta,
      String sourceRawText,
      ViewerTextRenderPlan renderPlan,
      String contentStyleClass,
      ViewerPresentationMode presentationMode,
      JsonBreadcrumbModel breadcrumbModel,
      JsonOutlineModel outlineModel) {
    return new ZoomViewerSnapshot(
        true,
        largePreview,
        windowTitle,
        modeLabel,
        documentTitle,
        documentMeta,
        sourceRawText == null ? "" : sourceRawText,
        renderPlan,
        contentStyleClass,
        presentationMode,
        breadcrumbModel == null ? JsonBreadcrumbModel.unavailable() : breadcrumbModel,
        outlineModel == null ? JsonOutlineModel.empty() : outlineModel,
        "");
  }

  public static ZoomViewerSnapshot empty(
      String windowTitle, String modeLabel, String documentTitle, String emptyStateMessage) {
    return new ZoomViewerSnapshot(
        false,
        false,
        windowTitle,
        modeLabel,
        documentTitle,
        "",
        "",
        null,
        "",
        ViewerPresentationMode.ASCII_TREE,
        JsonBreadcrumbModel.unavailable(),
        JsonOutlineModel.empty(),
        emptyStateMessage == null ? "" : emptyStateMessage);
  }
}
