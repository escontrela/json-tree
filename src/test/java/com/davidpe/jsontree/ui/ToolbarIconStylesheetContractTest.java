package com.davidpe.jsontree.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class ToolbarIconStylesheetContractTest {

  @Test
  void stylesheetDefinesSharedToolbarIconStatesForLightAndNightThemes() throws IOException {
    String css =
        new String(
            Objects.requireNonNull(
                    ToolbarIconStylesheetContractTest.class.getResourceAsStream(
                        "/com/davidpe/jsontree/ui/styles.css"))
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertTrue(css.contains(".toolbar-icon-button {"));
    assertTrue(css.contains(".toolbar-icon-button:hover"));
    assertTrue(css.contains(".toolbar-icon-button:pressed"));
    assertTrue(css.contains(".toolbar-icon-button:disabled"));
    assertTrue(css.contains(".toolbar-icon-button:toolbar-selected"));
    assertTrue(css.contains(".night-mode .toolbar-icon-button {"));
    assertTrue(css.contains(".night-mode .toolbar-icon-button:toolbar-selected"));
    assertTrue(css.contains(".viewer-toolbar-button-row .toolbar-icon-button"));
  }
}
