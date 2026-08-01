package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SearchPanelPositionerTest {

  private final SearchPanelPositioner positioner = new SearchPanelPositioner();

  @Test
  void anchorsThePanelNearTheTopRightByDefault() {
    SearchPanelPosition position = positioner.initialPosition(360.0, 180.0, 1200.0, 800.0);

    assertEquals(820.0, position.x(), 0.0001);
    assertEquals(20.0, position.y(), 0.0001);
  }

  @Test
  void clampsDraggedPositionsInsideTheVisibleViewport() {
    SearchPanelPosition position =
        positioner.clamp(-40.0, 900.0, 360.0, 180.0, 1200.0, 800.0);

    assertEquals(20.0, position.x(), 0.0001);
    assertEquals(600.0, position.y(), 0.0001);
  }

  @Test
  void preservesPointerOffsetSoThePanelDoesNotJumpOnDragStart() {
    SearchPanelPosition position =
        positioner.forDragPointer(300.0, 240.0, 40.0, 30.0, 360.0, 180.0, 1200.0, 800.0);

    assertEquals(260.0, position.x(), 0.0001);
    assertEquals(210.0, position.y(), 0.0001);
  }

  @Test
  void fallsBackToZeroMarginsWhenTheViewportIsTighterThanThePanelChrome() {
    SearchPanelPosition position = positioner.initialPosition(360.0, 180.0, 300.0, 160.0);

    assertEquals(0.0, position.x(), 0.0001);
    assertEquals(0.0, position.y(), 0.0001);
    assertTrue(position.x() >= 0.0);
    assertTrue(position.y() >= 0.0);
  }
}
