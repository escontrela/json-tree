package com.davidpe.jsontree.ui.support;

import java.util.List;
import java.util.Locale;
import javafx.scene.input.KeyCode;

/**
 * Centralizes the currently supported application shortcuts so runtime wiring and Settings copy do
 * not drift apart.
 */
public class ApplicationShortcutCatalog {

  private final boolean macOs;

  public ApplicationShortcutCatalog() {
    this(System.getProperty("os.name", ""));
  }

  ApplicationShortcutCatalog(String osName) {
    this.macOs = osName.toLowerCase(Locale.ROOT).contains("mac");
  }

  public List<SupportedShortcut> supportedShortcuts() {
    return List.of(
        new SupportedShortcut(
            "Open search panel",
            "Open or focus regex search for the current viewer when search is available.",
            searchPanelChordLabel()),
        new SupportedShortcut(
            "Paste clipboard document",
            "Import valid JSON or one supported curl command from the clipboard into the main workspace.",
            clipboardImportChordLabel()));
  }

  public boolean matchesSearchPanelShortcut(
      KeyCode keyCode, boolean shortcutDown, boolean altDown, boolean shiftDown) {
    return matches(keyCode, shortcutDown, altDown, shiftDown, KeyCode.F);
  }

  public boolean matchesClipboardImportShortcut(
      KeyCode keyCode, boolean shortcutDown, boolean altDown, boolean shiftDown) {
    return matches(keyCode, shortcutDown, altDown, shiftDown, KeyCode.P, KeyCode.V);
  }

  public String searchPanelChordLabel() {
    return formatChord("F");
  }

  public String clipboardImportChordLabel() {
    return formatChord("P") + " / " + formatChord("V");
  }

  private boolean matches(
      KeyCode keyCode,
      boolean shortcutDown,
      boolean altDown,
      boolean shiftDown,
      KeyCode... supportedKeys) {
    if (!shortcutDown || altDown || shiftDown || keyCode == null) {
      return false;
    }
    for (KeyCode supportedKey : supportedKeys) {
      if (supportedKey == keyCode) {
        return true;
      }
    }
    return false;
  }

  private String formatChord(String key) {
    return (macOs ? "Command+" : "Ctrl+") + key;
  }
}
