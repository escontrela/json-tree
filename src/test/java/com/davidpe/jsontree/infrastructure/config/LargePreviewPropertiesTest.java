package com.davidpe.jsontree.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LargePreviewPropertiesTest {

  @Test
  void exposesSafeLargePreviewDefaults() {
    LargePreviewProperties properties = new LargePreviewProperties();

    assertEquals(1_048_576L, properties.getFullRenderMaxBytes());
    assertEquals(400, properties.getPreviewMaxLines());
    assertEquals(400, properties.getPageLineCount());
    assertEquals(20, properties.getWarmPageRadius());
    assertEquals(8, properties.getPreviewMaxDepth());
    assertEquals(64, properties.getPreviewMaxChildrenPerContainer());
    assertEquals(12000, properties.getTextNodeBudget());
  }

  @Test
  void clampsWarmPageRadiusToSafeBounds() {
    LargePreviewProperties properties = new LargePreviewProperties();

    properties.setWarmPageRadius(-5);
    assertEquals(0, properties.getWarmPageRadius());

    properties.setWarmPageRadius(999);
    assertEquals(200, properties.getWarmPageRadius());
  }
}
