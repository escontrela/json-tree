package com.davidpe.jsontree.ui.support;

/**
 * Cyclic text-scale steps for the shared viewer surface.
 */
public enum ViewerTextScaleStep {
  BASE(1.0, "Viewer text size: default"),
  DOUBLE(2.0, "Viewer text size: 2x"),
  TRIPLE(3.0, "Viewer text size: 3x");

  private final double multiplier;
  private final String affordanceText;

  ViewerTextScaleStep(double multiplier, String affordanceText) {
    this.multiplier = multiplier;
    this.affordanceText = affordanceText;
  }

  public double multiplier() {
    return multiplier;
  }

  public String affordanceText() {
    return affordanceText;
  }

  public ViewerTextScaleStep next() {
    return switch (this) {
      case BASE -> DOUBLE;
      case DOUBLE -> TRIPLE;
      case TRIPLE -> BASE;
    };
  }
}
