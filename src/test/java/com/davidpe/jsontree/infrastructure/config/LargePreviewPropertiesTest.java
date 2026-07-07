package com.davidpe.jsontree.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LargePreviewPropertiesTest {

  @Test
  void exposesSafeLargePreviewDefaults() {
    LargePreviewProperties properties = new LargePreviewProperties();

    assertEquals(LargePreviewProperties.DEFAULT_FULL_RENDER_MAX_BYTES, properties.getFullRenderMaxBytes());
    assertEquals(400, properties.getPreviewMaxLines());
    assertEquals(400, properties.getPageLineCount());
    assertEquals(20, properties.getWarmPageRadius());
    assertEquals(8, properties.getPreviewMaxDepth());
    assertEquals(64, properties.getPreviewMaxChildrenPerContainer());
    assertEquals(12000, properties.getTextNodeBudget());
    assertEquals(512 * 1024, properties.getPageIndexStrideBytes());
    assertEquals(150 * 1024, properties.getVisibleChunkBytes());
    assertEquals(12 * 1024, properties.getChunkOverlapBytes());
  }

  @Test
  void clampsWarmPageRadiusToSafeBounds() {
    LargePreviewProperties properties = new LargePreviewProperties();

    properties.setFullRenderMaxBytes(0L);
    assertEquals(LargePreviewProperties.MIN_EDITABLE_BYTES, properties.getFullRenderMaxBytes());

    properties.setWarmPageRadius(-5);
    assertEquals(0, properties.getWarmPageRadius());

    properties.setWarmPageRadius(999);
    assertEquals(200, properties.getWarmPageRadius());

    properties.setVisibleChunkBytes(2048);
    properties.setChunkOverlapBytes(999999);
    assertEquals(1024, properties.getChunkOverlapBytes());
  }
}
