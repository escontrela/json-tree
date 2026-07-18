package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RichTextViewportScrollResolverTest {

  private final RichTextViewportScrollResolver resolver = new RichTextViewportScrollResolver();

  @Test
  void returnsSafeZeroWhenViewportDoesNotNeedScrolling() {
    assertEquals(0.0, resolver.scrollValue(80.0, 220.0, 180.0));
    assertEquals(0.0, resolver.scrollOffset(0.8, 220.0, 180.0));
  }

  @Test
  void mapsPixelOffsetsIntoNormalizedScrollValues() {
    assertEquals(0.0, resolver.scrollValue(0.0, 100.0, 500.0), 0.0001);
    assertEquals(0.5, resolver.scrollValue(200.0, 100.0, 500.0), 0.0001);
    assertEquals(1.0, resolver.scrollValue(400.0, 100.0, 500.0), 0.0001);
  }

  @Test
  void mapsNormalizedScrollValuesBackIntoPixelOffsets() {
    assertEquals(0.0, resolver.scrollOffset(0.0, 100.0, 500.0), 0.0001);
    assertEquals(200.0, resolver.scrollOffset(0.5, 100.0, 500.0), 0.0001);
    assertEquals(400.0, resolver.scrollOffset(1.0, 100.0, 500.0), 0.0001);
  }

  @Test
  void clampsOutOfRangeNormalizedValues() {
    assertEquals(0.0, resolver.scrollOffset(-1.0, 100.0, 500.0), 0.0001);
    assertEquals(400.0, resolver.scrollOffset(2.0, 100.0, 500.0), 0.0001);
  }
}
