package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

class SearchPanelShortcutSupportTest {

  private final SearchPanelShortcutSupport support = new SearchPanelShortcutSupport();

  @Test
  void triggersForShortcutModifierPlusFWhenSearchIsAvailable() {
    assertTrue(support.shouldTrigger(KeyCode.F, true, false, false, false, false, true));
  }

  @Test
  void ignoresNonMatchingKeysOrMissingShortcutModifier() {
    assertFalse(support.shouldTrigger(KeyCode.P, true, false, false, false, false, true));
    assertFalse(support.shouldTrigger(KeyCode.F, false, false, false, false, false, true));
  }

  @Test
  void ignoresShortcutWhenTypingOrWhenSearchIsBlocked() {
    assertFalse(support.shouldTrigger(KeyCode.F, true, false, false, true, false, true));
    assertFalse(support.shouldTrigger(KeyCode.F, true, false, false, false, true, true));
    assertFalse(support.shouldTrigger(KeyCode.F, true, false, false, false, false, false));
  }

  @Test
  void ignoresShiftedOrAlternativeVariants() {
    assertFalse(support.shouldTrigger(KeyCode.F, true, true, false, false, false, true));
    assertFalse(support.shouldTrigger(KeyCode.F, true, false, true, false, false, true));
  }
}
