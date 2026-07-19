package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainWindowBreadcrumbLayoutTest {

  @Test
  void placesBreadcrumbLabelBelowViewerToolbarButtonRow() throws IOException {
    String fxml =
        new String(
            MainWindowBreadcrumbLayoutTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/main.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    int actionsIndex = fxml.indexOf("styleClass=\"viewer-toolbar-actions\"");
    int buttonRowIndex = fxml.indexOf("styleClass=\"viewer-toolbar-button-row\"");
    int outlineIndex = fxml.indexOf("fx:id=\"outlineToggleButton\"");
    int breadcrumbIndex = fxml.indexOf("fx:id=\"breadcrumbLabel\"");

    assertTrue(actionsIndex >= 0);
    assertTrue(buttonRowIndex > actionsIndex);
    assertTrue(outlineIndex > buttonRowIndex);
    assertTrue(breadcrumbIndex > outlineIndex);
  }
}
