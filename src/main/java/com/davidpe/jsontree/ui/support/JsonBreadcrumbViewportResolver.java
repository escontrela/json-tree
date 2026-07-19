package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonBreadcrumbAnchor;
import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonBreadcrumbPath;
import com.davidpe.jsontree.ui.model.BreadcrumbViewerMode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JsonBreadcrumbViewportResolver {

  public Optional<JsonBreadcrumbPath> resolve(
      JsonBreadcrumbModel model, BreadcrumbViewerMode viewerMode, int visibleParagraphIndex) {
    if (model == null || !model.available() || model.anchors().isEmpty() || viewerMode == null) {
      return Optional.empty();
    }

    List<JsonBreadcrumbAnchor> anchors = model.anchors();
    int resolvedIndex = binarySearch(anchors, viewerMode, Math.max(0, visibleParagraphIndex));
    return resolvedIndex < 0
        ? Optional.empty()
        : Optional.of(anchors.get(resolvedIndex).path());
  }

  private int binarySearch(
      List<JsonBreadcrumbAnchor> anchors, BreadcrumbViewerMode viewerMode, int visibleParagraphIndex) {
    int low = 0;
    int high = anchors.size() - 1;
    int resolved = 0;

    while (low <= high) {
      int middle = (low + high) >>> 1;
      int anchorLine = lineIndexFor(anchors.get(middle), viewerMode);
      if (anchorLine <= visibleParagraphIndex) {
        resolved = middle;
        low = middle + 1;
      } else {
        high = middle - 1;
      }
    }
    return resolved;
  }

  private int lineIndexFor(JsonBreadcrumbAnchor anchor, BreadcrumbViewerMode viewerMode) {
    return switch (viewerMode) {
      case RAW_JSON -> anchor.rawDisplayLineIndex();
      case ASCII_TREE, STRUCTURE -> anchor.asciiLineIndex();
    };
  }
}
