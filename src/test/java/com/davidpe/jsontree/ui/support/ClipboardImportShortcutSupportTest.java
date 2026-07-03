package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

class ClipboardImportShortcutSupportTest {

  private final ClipboardImportShortcutSupport support = new ClipboardImportShortcutSupport();

  @Test
  void triggersForShortcutModifierPlusPOrVWithoutExtraModifiers() {
    assertTrue(support.shouldTrigger(KeyCode.P, true, false, false, false, false));
    assertTrue(support.shouldTrigger(KeyCode.V, true, false, false, false, false));
  }

  @Test
  void ignoresNonMatchingKeysOrMissingShortcutModifier() {
    assertFalse(support.shouldTrigger(KeyCode.F, true, false, false, false, false));
    assertFalse(support.shouldTrigger(KeyCode.P, false, false, false, false, false));
    assertFalse(support.shouldTrigger(KeyCode.V, false, false, false, false, false));
  }

  @Test
  void ignoresShortcutWhenTypingIntoTextInputOrWhenModalIsVisible() {
    assertFalse(support.shouldTrigger(KeyCode.P, true, false, false, true, false));
    assertFalse(support.shouldTrigger(KeyCode.V, true, false, false, true, false));
    assertFalse(support.shouldTrigger(KeyCode.P, true, false, false, false, true));
    assertFalse(support.shouldTrigger(KeyCode.V, true, false, false, false, true));
  }

  @Test
  void ignoresShiftedOrAlternativeShortcutVariants() {
    assertFalse(support.shouldTrigger(KeyCode.P, true, true, false, false, false));
    assertFalse(support.shouldTrigger(KeyCode.P, true, false, true, false, false));
    assertFalse(support.shouldTrigger(KeyCode.V, true, true, false, false, false));
    assertFalse(support.shouldTrigger(KeyCode.V, true, false, true, false, false));
  }
}
