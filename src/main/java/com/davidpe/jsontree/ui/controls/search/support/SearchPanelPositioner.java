package com.davidpe.jsontree.ui.controls.search.support;

import com.davidpe.jsontree.ui.controls.search.model.SearchPanelPosition;

/**
 * Computes bounded search-panel positions inside the visible main workspace.
 */
public class SearchPanelPositioner {

  private static final double EDGE_MARGIN = 20.0;

  public SearchPanelPosition initialPosition(
      double panelWidth, double panelHeight, double viewportWidth, double viewportHeight) {
    return clamp(
        viewportWidth - panelWidth - EDGE_MARGIN,
        EDGE_MARGIN,
        panelWidth,
        panelHeight,
        viewportWidth,
        viewportHeight);
  }

  public SearchPanelPosition forDragPointer(
      double pointerX,
      double pointerY,
      double pointerOffsetX,
      double pointerOffsetY,
      double panelWidth,
      double panelHeight,
      double viewportWidth,
      double viewportHeight) {
    return clamp(
        pointerX - pointerOffsetX,
        pointerY - pointerOffsetY,
        panelWidth,
        panelHeight,
        viewportWidth,
        viewportHeight);
  }

  public SearchPanelPosition clamp(
      double desiredX,
      double desiredY,
      double panelWidth,
      double panelHeight,
      double viewportWidth,
      double viewportHeight) {
    double minX = boundedEdgeStart(viewportWidth, panelWidth);
    double maxX = boundedEdgeEnd(viewportWidth, panelWidth);
    double minY = boundedEdgeStart(viewportHeight, panelHeight);
    double maxY = boundedEdgeEnd(viewportHeight, panelHeight);
    return new SearchPanelPosition(
        clampAxis(desiredX, minX, maxX),
        clampAxis(desiredY, minY, maxY));
  }

  private double boundedEdgeStart(double viewportSize, double panelSize) {
    return viewportSize >= panelSize + (EDGE_MARGIN * 2.0) ? EDGE_MARGIN : 0.0;
  }

  private double boundedEdgeEnd(double viewportSize, double panelSize) {
    if (viewportSize >= panelSize + (EDGE_MARGIN * 2.0)) {
      return Math.max(EDGE_MARGIN, viewportSize - panelSize - EDGE_MARGIN);
    }
    return Math.max(0.0, viewportSize - panelSize);
  }

  private double clampAxis(double value, double minValue, double maxValue) {
    if (maxValue <= minValue) {
      return minValue;
    }
    return Math.max(minValue, Math.min(maxValue, value));
  }
}
