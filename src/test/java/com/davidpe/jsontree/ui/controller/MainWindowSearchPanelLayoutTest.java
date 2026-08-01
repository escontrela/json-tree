package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainWindowSearchPanelLayoutTest {

  @Test
  void replacesTheLegacySearchStripAndModalWithTheFloatingOverlayPane() throws IOException {
    String fxml =
        new String(
            MainWindowSearchPanelLayoutTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/main.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertTrue(fxml.contains("fx:id=\"workspaceOverlayPane\""));
    assertTrue(fxml.contains("onAction=\"#openSearchPanel\""));
    assertFalse(fxml.contains("fx:id=\"activeSearchStrip\""));
    assertFalse(fxml.contains("fx:id=\"searchModalCard\""));
  }
}
