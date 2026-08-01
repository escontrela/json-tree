package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ZoomWindowLayoutContractTest {

  @Test
  void keepsTheZoomWindowFocusedOnReaderChromeOnly() throws IOException {
    String fxml =
        new String(
            ZoomWindowLayoutContractTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/zoom.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertTrue(fxml.contains("fx:id=\"zoomViewerHost\""));
    assertTrue(fxml.contains("fx:id=\"zoomSearchButton\""));
    assertTrue(fxml.contains("fx:id=\"zoomOutlineToggleButton\""));
    assertTrue(fxml.contains("fx:id=\"zoomOverlayPane\""));
    assertTrue(fxml.contains("fx:id=\"zoomBreadcrumbLabel\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/close_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/close_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/outline_35dp_FFFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/outline_35dp_000000.png\""));
    assertTrue(fxml.contains("toggleMode=\"true\""));
    assertTrue(fxml.contains("selected=\"true\""));
    assertTrue(fxml.contains("onAction=\"#openSearchPanel\""));
    assertFalse(fxml.contains("searchButton"));
    assertFalse(fxml.contains("activeSearchStrip"));
    assertFalse(fxml.contains("fx:id=\"zoomSearchField\""));
    assertFalse(fxml.contains("fx:id=\"zoomCropButton\""));
    assertFalse(fxml.contains("historyListView"));
  }
}
