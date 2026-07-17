package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import org.springframework.stereotype.Component;

/**
 * Derives the visible state for large-preview page navigation controls from the active viewer
 * result without leaking controller-specific branching into JavaFX event handlers.
 */
@Component
public class LargePreviewPageNavigationStateResolver {

  public LargePreviewPageNavigationState resolve(JsonViewerLoadResult result) {
    if (result == null
        || !result.usesLargePreview()
        || !result.hasLargePreviewSession()
        || !result.largePreviewSession().totalPagesKnown()) {
      return LargePreviewPageNavigationState.hidden();
    }

    int currentPageNumber = result.largePreviewSession().currentPageIndex() + 1;
    int totalPages = result.largePreviewSession().totalPages();
    return new LargePreviewPageNavigationState(
        true,
        currentPageNumber,
        totalPages,
        currentPageNumber > 1,
        currentPageNumber < totalPages);
  }
}
