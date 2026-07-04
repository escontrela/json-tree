package com.davidpe.jsontree.ui.support;

public record OutlineViewportProjection(
    boolean visible,
    double y,
    double height
) {

  public static OutlineViewportProjection hidden() {
    return new OutlineViewportProjection(false, 0.0, 0.0);
  }
}
