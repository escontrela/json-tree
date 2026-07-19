package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainWindowStructureButtonLayoutTest {

  @Test
  void placesStructureButtonImmediatelyAfterRawJsonAndBeforeSearch() throws IOException {
    String fxml =
        new String(
            MainWindowStructureButtonLayoutTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/main.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    int rawIndex = fxml.indexOf("fx:id=\"rawJsonButton\"");
    int structureIndex = fxml.indexOf("fx:id=\"structureButton\"");
    int searchIndex = fxml.indexOf("fx:id=\"searchButton\"");

    assertTrue(rawIndex >= 0);
    assertTrue(structureIndex > rawIndex);
    assertTrue(searchIndex > structureIndex);
  }
}
