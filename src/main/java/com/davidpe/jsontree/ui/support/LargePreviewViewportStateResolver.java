package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the shared large-preview viewport state from either a global scroll value or an
 * explicit page target using the persisted page-range metadata of the active session.
 */
@Component
public class LargePreviewViewportStateResolver {

  public Optional<LargePreviewViewportState> resolveForScroll(
      JsonViewerLoadResult result, double globalScrollValue) {
    if (!supportsLargeViewport(result)) {
      return Optional.empty();
    }
    return result.largePreviewSession().resolvePageIndexForScrollValue(globalScrollValue).stream()
        .mapToObj(
            pageIndex ->
                new LargePreviewViewportState(
                    true,
                    pageIndex,
                    result.largePreviewSession().totalPages(),
                    globalScrollValue))
        .findFirst();
  }

  public Optional<LargePreviewViewportState> resolveForPage(
      JsonViewerLoadResult result, int targetPageIndex) {
    if (!supportsLargeViewport(result)
        || targetPageIndex < 0
        || targetPageIndex >= result.largePreviewSession().totalPages()) {
      return Optional.empty();
    }
    return Optional.of(
        new LargePreviewViewportState(
            true,
            targetPageIndex,
            result.largePreviewSession().totalPages(),
            result.largePreviewSession().scrollValueForPageStart(targetPageIndex)));
  }

  public LargePreviewViewportState inactive() {
    return LargePreviewViewportState.inactive();
  }

  private boolean supportsLargeViewport(JsonViewerLoadResult result) {
    return result != null
        && result.usesLargePreview()
        && result.hasLargePreviewSession()
        && result.largePreviewSession().totalPagesKnown();
  }
}
