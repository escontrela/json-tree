package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutlineViewportProjectorTest {

  private final OutlineViewportProjector projector = new OutlineViewportProjector();

  @Test
  void projectsFullHeightMarkerWhenEntireDocumentIsVisible() {
    OutlineViewportProjection projection = projector.project(0.0, 220.0, 320.0, 180.0);

    assertTrue(projection.visible());
    assertEquals(0.0, projection.y());
    assertEquals(220.0, projection.height());
  }

  @Test
  void projectsMarkerPositionFromScrollValue() {
    OutlineViewportProjection projection = projector.project(0.5, 220.0, 80.0, 400.0);

    assertTrue(projection.visible());
    assertTrue(projection.y() > 0.0);
    assertTrue(projection.height() >= 24.0);
  }

  @Test
  void movesMarkerForwardWhenScrollValueAdvances() {
    OutlineViewportProjection firstProjection = projector.project(0.20, 220.0, 80.0, 400.0);
    OutlineViewportProjection secondProjection = projector.project(0.80, 220.0, 80.0, 400.0);

    assertTrue(secondProjection.y() > firstProjection.y());
    assertEquals(firstProjection.height(), secondProjection.height());
  }

  @Test
  void hidesMarkerWhenGeometryIsUnavailable() {
    OutlineViewportProjection projection = projector.project(0.0, 0.0, 0.0, 0.0);

    assertFalse(projection.visible());
  }
}
