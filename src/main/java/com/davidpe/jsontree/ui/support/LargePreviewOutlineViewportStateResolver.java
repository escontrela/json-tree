package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves a large-preview outline pointer interaction into a deterministic page-based viewport
 * state by snapping the minimap pointer onto the persisted page-range anchors of the active
 * session.
 */
@Component
public class LargePreviewOutlineViewportStateResolver {

  private final OutlineMinimapScrollMapper outlineMinimapScrollMapper;
  private final LargePreviewViewportStateResolver largePreviewViewportStateResolver;

  public LargePreviewOutlineViewportStateResolver(
      OutlineMinimapScrollMapper outlineMinimapScrollMapper,
      LargePreviewViewportStateResolver largePreviewViewportStateResolver) {
    this.outlineMinimapScrollMapper = outlineMinimapScrollMapper;
    this.largePreviewViewportStateResolver = largePreviewViewportStateResolver;
  }

  public Optional<LargePreviewViewportState> resolveForPointer(
      JsonViewerLoadResult result,
      double pointerY,
      double minimapHeight,
      double viewportHeight,
      double contentHeight) {
    double globalScrollValue =
        outlineMinimapScrollMapper.scrollValueForPointer(
            pointerY, minimapHeight, viewportHeight, contentHeight);
    return largePreviewViewportStateResolver
        .resolveForScroll(result, globalScrollValue)
        .flatMap(
            targetState ->
                largePreviewViewportStateResolver.resolveForPage(
                    result, targetState.currentPageIndex()));
  }
}
