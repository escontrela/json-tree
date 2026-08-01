package com.davidpe.jsontree.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SearchPanelStylesheetContractTest {

  @Test
  void keepsOnlyTheFloatingSearchPanelSelectorsAndNightModeHooks() throws IOException {
    String css = readStylesheet();

    assertTrue(css.contains(".search-panel-card"));
    assertTrue(css.contains(".search-panel-helper-error"));
    assertTrue(css.contains(".night-mode .search-panel-card"));
    assertFalse(css.contains(".search-modal-card"));
    assertFalse(css.contains(".active-search-strip"));
    assertFalse(css.contains(".search-strip-button"));
  }

  private static String readStylesheet() throws IOException {
    try (InputStream stream =
        SearchPanelStylesheetContractTest.class.getResourceAsStream(
            "/com/davidpe/jsontree/ui/styles.css")) {
      if (stream == null) {
        throw new IOException("styles.css not found on the classpath.");
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
