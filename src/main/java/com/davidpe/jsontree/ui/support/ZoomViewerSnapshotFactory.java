package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import org.springframework.stereotype.Component;

/**
 * Builds presentation snapshots consumed by the secondary zoom window.
 */
@Component
public class ZoomViewerSnapshotFactory {

  public ZoomViewerSnapshot renderable(
      JsonViewerLoadResult result,
      String modeLabel,
      ViewerTextRenderPlan renderPlan,
      String contentStyleClass,
      String fileMeta,
      ViewerPresentationMode presentationMode,
      JsonBreadcrumbModel breadcrumbModel) {
    String windowTitle = "JSON -> TREE • Zoom • " + result.importResult().fileName();
    return ZoomViewerSnapshot.renderable(
        result.usesLargePreview(),
        windowTitle,
        modeLabel,
        result.importResult().fileName(),
        fileMeta,
        renderPlan,
        contentStyleClass,
        presentationMode,
        breadcrumbModel);
  }
}
