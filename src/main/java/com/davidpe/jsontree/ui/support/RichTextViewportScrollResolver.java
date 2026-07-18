package com.davidpe.jsontree.ui.support;

/**
 * Maps RichTextFX pixel-based viewport positions onto normalized scroll values used by the outline
 * minimap.
 */
public class RichTextViewportScrollResolver {

  public double scrollValue(double estimatedScrollY, double viewportHeight, double contentHeight) {
    double maxScrollOffset = maxScrollOffset(viewportHeight, contentHeight);
    if (maxScrollOffset <= 0.0) {
      return 0.0;
    }
    return clamp(estimatedScrollY / maxScrollOffset);
  }

  public double scrollOffset(double scrollValue, double viewportHeight, double contentHeight) {
    double maxScrollOffset = maxScrollOffset(viewportHeight, contentHeight);
    if (maxScrollOffset <= 0.0) {
      return 0.0;
    }
    return clamp(scrollValue) * maxScrollOffset;
  }

  private double maxScrollOffset(double viewportHeight, double contentHeight) {
    if (viewportHeight <= 0.0 || contentHeight <= 0.0 || contentHeight <= viewportHeight) {
      return 0.0;
    }
    return contentHeight - viewportHeight;
  }

  private double clamp(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return 0.0;
    }
    return Math.max(0.0, Math.min(1.0, value));
  }
}
