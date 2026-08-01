package com.davidpe.jsontree.ui.support;

import javafx.scene.input.KeyCode;

/**
 * Guards the standard search shortcut so it only fires in the main viewer when the current
 * document can actually open the floating search panel.
 */
public class SearchPanelShortcutSupport {

  private final ApplicationShortcutCatalog shortcutCatalog = new ApplicationShortcutCatalog();

  public boolean shouldTrigger(
      KeyCode keyCode,
      boolean shortcutDown,
      boolean altDown,
      boolean shiftDown,
      boolean textInputTarget,
      boolean searchQueryEditing,
      boolean searchAvailable) {
    if (!searchAvailable || textInputTarget || searchQueryEditing) {
      return false;
    }
    return shortcutCatalog.matchesSearchPanelShortcut(
        keyCode, shortcutDown, altDown, shiftDown);
  }
}
