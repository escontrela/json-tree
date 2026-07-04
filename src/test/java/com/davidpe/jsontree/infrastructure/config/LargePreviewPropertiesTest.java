package com.davidpe.jsontree.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LargePreviewPropertiesTest {

  @Test
  void exposesSafeLargePreviewDefaults() {
    LargePreviewProperties properties = new LargePreviewProperties();

    assertEquals(1_048_576L, properties.getFullRenderMaxBytes());
    assertEquals(400, properties.getPreviewMaxLines());
    assertEquals(8, properties.getPreviewMaxDepth());
    assertEquals(64, properties.getPreviewMaxChildrenPerContainer());
    assertEquals(12000, properties.getTextNodeBudget());
  }
}
