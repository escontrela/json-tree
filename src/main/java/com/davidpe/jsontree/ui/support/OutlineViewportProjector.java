package com.davidpe.jsontree.ui.support;

import org.springframework.stereotype.Component;

@Component
public class OutlineViewportProjector {

  private static final double MIN_MARKER_HEIGHT = 24.0;

  public OutlineViewportProjection project(
      double scrollValue,
      double minimapHeight,
      double viewportHeight,
      double contentHeight
  ) {
    if (minimapHeight <= 0.0 || viewportHeight <= 0.0 || contentHeight <= 0.0) {
      return OutlineViewportProjection.hidden();
    }

    double viewportRatio =
        contentHeight <= viewportHeight
            ? 1.0
            : clamp(viewportHeight / contentHeight);
    double markerHeight = Math.min(minimapHeight, Math.max(MIN_MARKER_HEIGHT, minimapHeight * viewportRatio));
    double travel = Math.max(0.0, minimapHeight - markerHeight);
    double y = clamp(scrollValue) * travel;
    return new OutlineViewportProjection(true, y, markerHeight);
  }

  private double clamp(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
