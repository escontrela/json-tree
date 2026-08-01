package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationShortcutCatalogTest {

  @Test
  void formatsSupportedShortcutsForMacOs() {
    ApplicationShortcutCatalog catalog = new ApplicationShortcutCatalog("Mac OS X");

    List<SupportedShortcut> shortcuts = catalog.supportedShortcuts();

    assertEquals("Command+F", shortcuts.get(0).chordLabel());
    assertEquals("Command+P / Command+V", shortcuts.get(1).chordLabel());
  }

  @Test
  void formatsSupportedShortcutsForNonMacPlatforms() {
    ApplicationShortcutCatalog catalog = new ApplicationShortcutCatalog("Linux");

    List<SupportedShortcut> shortcuts = catalog.supportedShortcuts();

    assertEquals("Ctrl+F", shortcuts.get(0).chordLabel());
    assertEquals("Ctrl+P / Ctrl+V", shortcuts.get(1).chordLabel());
  }

  @Test
  void exposesOnlyCurrentlyImplementedGlobalShortcuts() {
    ApplicationShortcutCatalog catalog = new ApplicationShortcutCatalog("Mac OS X");

    List<SupportedShortcut> shortcuts = catalog.supportedShortcuts();

    assertEquals(2, shortcuts.size());
    assertEquals("Open search panel", shortcuts.get(0).title());
    assertEquals("Paste clipboard document", shortcuts.get(1).title());
    assertTrue(shortcuts.get(1).description().contains("supported curl command"));
  }
}
