package com.davidpe.jsontree.ui.support;

import org.springframework.stereotype.Component;

@Component
public class OutlineMinimapScrollMapper {

  public double scrollValueForPointer(
      double pointerY,
      double minimapHeight,
      double viewportHeight,
      double contentHeight
  ) {
    if (minimapHeight <= 0.0
        || viewportHeight <= 0.0
        || contentHeight <= 0.0
        || contentHeight <= viewportHeight) {
      return 0.0;
    }

    double viewportRatio = clamp(viewportHeight / contentHeight);
    double targetRatio = clamp(pointerY / minimapHeight);
    double scrollableRatio = 1.0 - viewportRatio;
    if (scrollableRatio <= 0.0) {
      return 0.0;
    }

    double centeredTarget = targetRatio - (viewportRatio / 2.0);
    return clamp(centeredTarget / scrollableRatio);
  }

  private double clamp(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
