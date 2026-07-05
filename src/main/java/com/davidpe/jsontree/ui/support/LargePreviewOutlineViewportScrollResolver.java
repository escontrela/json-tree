package com.davidpe.jsontree.ui.support;

import org.springframework.stereotype.Component;

/**
 * Resolves the internal scroll position required to keep the active large-preview outline anchor
 * visible when the outline rail contains more steps than fit in the viewport.
 */
@Component
public class LargePreviewOutlineViewportScrollResolver {

  public double scrollValueForReveal(
      double childY, double childHeight, double viewportHeight, double contentHeight) {
    if (contentHeight <= viewportHeight || viewportHeight <= 0.0) {
      return 0.0;
    }

    double targetOffset =
        Math.max(
            0.0,
            Math.min(
                childY - Math.max(0.0, (viewportHeight - childHeight) / 2.0),
                contentHeight - viewportHeight));
    return targetOffset / Math.max(1.0, contentHeight - viewportHeight);
  }
}
