package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainWindowStructureButtonLayoutTest {

  @Test
  void movesStructureAndCropIntoTheViewerToolbarWhileKeepingSettingsInTheHeader() throws IOException {
    String fxml =
        new String(
            MainWindowStructureButtonLayoutTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/main.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    int headerIndex = fxml.indexOf("styleClass=\"header-action-bar\"");
    int settingsIndex = fxml.indexOf("fx:id=\"settingsButton\"");
    int buttonRowIndex = fxml.indexOf("styleClass=\"viewer-toolbar-button-row\"");
    int structureIndex = fxml.indexOf("fx:id=\"structureButton\"");
    int cropIndex = fxml.indexOf("fx:id=\"cropButton\"");

    assertTrue(headerIndex >= 0);
    assertTrue(settingsIndex > headerIndex);
    assertTrue(buttonRowIndex > settingsIndex);
    assertTrue(structureIndex > buttonRowIndex);
    assertTrue(cropIndex > structureIndex);
  }
}
