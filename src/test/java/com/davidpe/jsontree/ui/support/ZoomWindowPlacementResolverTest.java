package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.ui.service.StageZoomWindowCoordinator;
import org.junit.jupiter.api.Test;

class ZoomWindowPlacementResolverTest {

  private final ZoomWindowPlacementResolver resolver = new ZoomWindowPlacementResolver();

  @Test
  void centersZoomWindowWithinOwnerBounds() {
    assertEquals(137.0, resolver.centeredCoordinate(80.0, 1294.0, 1180.0), 0.0001);
    assertEquals(59.0, resolver.centeredCoordinate(40.0, 798.0, 760.0), 0.0001);
  }

  @Test
  void preservesTheControlledInitialZoomWindowSizeContract() {
    assertEquals(1180.0, StageZoomWindowCoordinator.INITIAL_WIDTH, 0.0001);
    assertEquals(760.0, StageZoomWindowCoordinator.INITIAL_HEIGHT, 0.0001);
  }
}
