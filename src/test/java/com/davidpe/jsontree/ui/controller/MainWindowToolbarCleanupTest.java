package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainWindowToolbarCleanupTest {

  @Test
  void removesInactiveFocusAndDensityButtonsFromTheViewerToolbar() throws IOException {
    String fxml =
        new String(
            MainWindowToolbarCleanupTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/main.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertFalse(fxml.contains("text=\"Focus\""));
    assertFalse(fxml.contains("text=\"Density\""));
    assertTrue(fxml.contains("fx:id=\"zoomButton\""));
    assertTrue(fxml.contains("fx:id=\"outlineToggleButton\""));
  }
}
