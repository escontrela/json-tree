package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutlinePanelVisibilityResolverTest {

  private final OutlinePanelVisibilityResolver resolver = new OutlinePanelVisibilityResolver();

  @Test
  void hidesOutlineTheFirstTimeALargePreviewDocumentAppears() {
    OutlinePanelVisibilityState state =
        resolver.resolve(true, true, "history:2026-07-17_large.json", null);

    assertFalse(state.visible());
    assertEquals("history:2026-07-17_large.json", state.autoHiddenIdentity());
  }

  @Test
  void preservesManualReopenWithinTheSameLargePreviewDocument() {
    OutlinePanelVisibilityState state =
        resolver.resolve(
            true,
            true,
            "history:2026-07-17_large.json",
            "history:2026-07-17_large.json");

    assertTrue(state.visible());
    assertEquals("history:2026-07-17_large.json", state.autoHiddenIdentity());
  }

  @Test
  void clearsAutoHiddenIdentityOutsideLargePreview() {
    OutlinePanelVisibilityState state =
        resolver.resolve(false, false, "file:/tmp/small.json", "history:2026-07-17_large.json");

    assertFalse(state.visible());
    assertNull(state.autoHiddenIdentity());
  }
}
