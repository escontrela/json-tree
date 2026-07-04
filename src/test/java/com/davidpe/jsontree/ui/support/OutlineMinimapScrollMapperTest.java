package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutlineMinimapScrollMapperTest {

  private final OutlineMinimapScrollMapper mapper = new OutlineMinimapScrollMapper();

  @Test
  void mapsTopAndBottomPointerPositionsIntoScrollRange() {
    assertEquals(0.0, mapper.scrollValueForPointer(0.0, 200.0, 40.0, 400.0));
    assertEquals(1.0, mapper.scrollValueForPointer(200.0, 200.0, 40.0, 400.0));
  }

  @Test
  void centersViewportAroundIntermediatePointerPositions() {
    double scrollValue = mapper.scrollValueForPointer(100.0, 200.0, 40.0, 400.0);

    assertEquals(0.5, scrollValue, 0.0001);
  }

  @Test
  void returnsSafeZeroWhenDocumentDoesNotNeedScrolling() {
    double scrollValue = mapper.scrollValueForPointer(160.0, 200.0, 220.0, 180.0);

    assertEquals(0.0, scrollValue);
  }

  @Test
  void clampsPointerValuesOutsideTheMinimapBounds() {
    double low = mapper.scrollValueForPointer(-20.0, 200.0, 40.0, 400.0);
    double high = mapper.scrollValueForPointer(260.0, 200.0, 40.0, 400.0);

    assertEquals(0.0, low);
    assertTrue(high <= 1.0);
  }
}
