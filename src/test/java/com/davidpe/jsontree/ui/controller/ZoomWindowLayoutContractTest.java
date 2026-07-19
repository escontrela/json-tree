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
    assertTrue(fxml.contains("fx:id=\"zoomSearchField\""));
    assertTrue(fxml.contains("fx:id=\"zoomSearchPreviousButton\""));
    assertTrue(fxml.contains("fx:id=\"zoomSearchNextButton\""));
    assertTrue(fxml.contains("fx:id=\"zoomBreadcrumbLabel\""));
    assertTrue(fxml.contains("text=\"Close\""));
    assertFalse(fxml.contains("outlineToggleButton"));
    assertFalse(fxml.contains("searchButton"));
    assertFalse(fxml.contains("activeSearchStrip"));
    assertFalse(fxml.contains("historyListView"));
  }
}
