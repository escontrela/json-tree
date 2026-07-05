package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewPageRange;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds page-based outline anchors for large-preview sessions from document-wide page ranges
 * instead of deriving interaction from sampled minimap buckets.
 */
@Component
public class LargePreviewOutlineStepResolver {

  public List<LargePreviewOutlineStep> resolve(JsonViewerLoadResult result) {
    if (result == null || !result.usesLargePreview() || !result.hasLargePreviewSession()) {
      return List.of();
    }

    return result.largePreviewSession().pageRanges().stream()
        .map(pageRange -> toStep(result, pageRange))
        .toList();
  }

  private LargePreviewOutlineStep toStep(
      JsonViewerLoadResult result, LargePreviewPageRange pageRange) {
    long startingLine = pageRange.startingLogicalLine() + 1L;
    long endingLine = pageRange.endingLogicalLineExclusive();
    int pageNumber = pageRange.pageIndex() + 1;
    return new LargePreviewOutlineStep(
        pageRange.pageIndex(),
        "Page " + pageNumber,
        "Lines " + startingLine + "-" + endingLine,
        pageRange.pageIndex() == result.largePreviewSession().currentPageIndex(),
        result.largePreviewSession().scrollValueForPageStart(pageRange.pageIndex()));
  }
}
