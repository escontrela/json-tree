package com.davidpe.jsontree.ui.controls.toolbar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ToolbarIconAssetResolverTest {

  private final ToolbarIconAssetResolver resolver = new ToolbarIconAssetResolver();

  @Test
  void resolvesTheLightAssetOutsideNightMode() {
    assertEquals(
        "/com/davidpe/jsontree/images/zoom_35dp_000000.png",
        resolver.resolve(
            "/com/davidpe/jsontree/images/zoom_35dp_000000.png",
            "/com/davidpe/jsontree/images/zoom_35dp_FFFFFF.png",
            false));
  }

  @Test
  void resolvesTheDarkAssetInNightMode() {
    assertEquals(
        "/com/davidpe/jsontree/images/outline_35dp_FFFFFFF.png",
        resolver.resolve(
            "/com/davidpe/jsontree/images/outline_35dp_000000.png",
            "/com/davidpe/jsontree/images/outline_35dp_FFFFFFF.png",
            true));
  }
}
