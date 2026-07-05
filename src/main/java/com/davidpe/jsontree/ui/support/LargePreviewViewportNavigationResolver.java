package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves every large-preview navigation entry point onto the same shared viewport state so the
 * controller can keep outline interaction, viewer scrolling, and page controls synchronized
 * through one deterministic page model.
 */
@Component
public class LargePreviewViewportNavigationResolver {

  private final LargePreviewViewportStateResolver largePreviewViewportStateResolver;
  private final LargePreviewOutlineViewportStateResolver largePreviewOutlineViewportStateResolver;

  public LargePreviewViewportNavigationResolver(
      LargePreviewViewportStateResolver largePreviewViewportStateResolver,
      LargePreviewOutlineViewportStateResolver largePreviewOutlineViewportStateResolver) {
    this.largePreviewViewportStateResolver = largePreviewViewportStateResolver;
    this.largePreviewOutlineViewportStateResolver = largePreviewOutlineViewportStateResolver;
  }

  public Optional<LargePreviewViewportState> resolveForScroll(
      JsonViewerLoadResult result, double globalScrollValue) {
    return largePreviewViewportStateResolver.resolveForScroll(result, globalScrollValue);
  }

  public Optional<LargePreviewViewportState> resolveForPage(
      JsonViewerLoadResult result, int pageIndex) {
    return largePreviewViewportStateResolver.resolveForPage(result, pageIndex);
  }

  public Optional<LargePreviewViewportState> resolveForRelativePage(
      JsonViewerLoadResult result, LargePreviewViewportState currentViewportState, int pageDelta) {
    if (result == null
        || currentViewportState == null
        || !currentViewportState.active()
        || !result.hasLargePreviewSession()) {
      return Optional.empty();
    }
    int targetPageIndex =
        Math.max(
            0,
            Math.min(
                currentViewportState.currentPageIndex() + pageDelta,
                result.largePreviewSession().totalPages() - 1));
    return resolveForPage(result, targetPageIndex);
  }

  public Optional<LargePreviewViewportState> resolveForOutlinePointer(
      JsonViewerLoadResult result,
      double pointerY,
      double minimapHeight,
      double viewportHeight,
      double contentHeight) {
    return largePreviewOutlineViewportStateResolver.resolveForPointer(
        result, pointerY, minimapHeight, viewportHeight, contentHeight);
  }

  public LargePreviewViewportState inactive() {
    return largePreviewViewportStateResolver.inactive();
  }
}
