package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainWindowZoomButtonLayoutTest {

  @Test
  void placesZoomButtonImmediatelyBeforeOutlineToggleButton() throws IOException {
    String fxml =
        new String(
            MainWindowZoomButtonLayoutTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/main.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    int zoomIndex = fxml.indexOf("fx:id=\"zoomButton\"");
    int outlineIndex = fxml.indexOf("fx:id=\"outlineToggleButton\"");

    assertTrue(zoomIndex >= 0);
    assertTrue(outlineIndex > zoomIndex);
    assertTrue(fxml.contains("<ToolbarIconButton fx:id=\"zoomButton\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/zoom_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/zoom_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("tooltipText=\"Open zoom viewer\""));
    assertTrue(fxml.contains("accessibleText=\"Open zoom viewer\""));
  }
}
