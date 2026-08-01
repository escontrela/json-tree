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
    assertTrue(fxml.contains("<ToolbarIconButton fx:id=\"zoomButton\""));
    assertTrue(fxml.contains("<ToolbarIconButton fx:id=\"outlineToggleButton\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/outline_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/outline_35dp_FFFFFFF.png\""));
    assertTrue(fxml.contains("toggleMode=\"true\""));
    assertTrue(fxml.contains("tooltipText=\"Toggle outline panel\""));
    assertTrue(fxml.contains("tooltipText=\"Open zoom viewer\""));
  }
}
