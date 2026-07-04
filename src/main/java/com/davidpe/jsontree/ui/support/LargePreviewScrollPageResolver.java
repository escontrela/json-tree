package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import java.util.OptionalInt;
import org.springframework.stereotype.Component;

/**
 * Pure UI helper that decides whether the large-preview viewer should request the previous or next
 * page based on the current scroll position.
 */
@Component
public class LargePreviewScrollPageResolver {

  private static final double PREVIOUS_PAGE_THRESHOLD = 0.02;
  private static final double NEXT_PAGE_THRESHOLD = 0.98;

  public OptionalInt targetPage(JsonViewerLoadResult result, double verticalScrollValue) {
    if (result == null || !result.usesLargePreview() || !result.hasLargePreviewSession()) {
      return OptionalInt.empty();
    }

    int currentPageIndex = result.largePreviewSession().currentPageIndex();
    if (verticalScrollValue <= PREVIOUS_PAGE_THRESHOLD && currentPageIndex > 0) {
      return OptionalInt.of(currentPageIndex - 1);
    }
    if (verticalScrollValue >= NEXT_PAGE_THRESHOLD && canAdvance(result)) {
      return OptionalInt.of(currentPageIndex + 1);
    }
    return OptionalInt.empty();
  }

  private boolean canAdvance(JsonViewerLoadResult result) {
    if (result.largePreviewSession().totalPagesKnown()) {
      return result.largePreviewSession().currentPageIndex() + 1 < result.largePreviewSession().totalPages();
    }
    return true;
  }
}
