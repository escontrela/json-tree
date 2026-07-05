package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LargePreviewOutlineViewportScrollResolverTest {

  private final LargePreviewOutlineViewportScrollResolver resolver =
      new LargePreviewOutlineViewportScrollResolver();

  @Test
  void keepsScrollAtTopWhenContentFitsViewport() {
    assertEquals(0.0, resolver.scrollValueForReveal(24.0, 40.0, 320.0, 280.0));
  }

  @Test
  void centersActiveStepWhenOutlineOverflowsViewport() {
    double scrollValue = resolver.scrollValueForReveal(540.0, 44.0, 240.0, 1_200.0);

    assertTrue(scrollValue > 0.0);
    assertTrue(scrollValue < 1.0);
  }
}
