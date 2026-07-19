package com.davidpe.jsontree.ui.support;

import org.springframework.stereotype.Component;

/**
 * Computes deterministic placement coordinates for the secondary zoom window.
 */
@Component
public class ZoomWindowPlacementResolver {

  public double centeredCoordinate(double ownerCoordinate, double ownerExtent, double windowExtent) {
    return ownerCoordinate + Math.max(0.0, (ownerExtent - windowExtent) / 2.0);
  }
}
