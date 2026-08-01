package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SearchPanelLayoutContractTest {

  @Test
  void exposesTheDedicatedDragHandleAndSearchActions() throws IOException {
    String fxml =
        new String(
            SearchPanelLayoutContractTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/controls/search/search-panel.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertTrue(fxml.contains("fx:id=\"searchPanelDragHandle\""));
    assertTrue(fxml.contains("fx:id=\"searchPanelQueryField\""));
    assertTrue(fxml.contains("fx:id=\"searchPanelPreviousButton\""));
    assertTrue(fxml.contains("fx:id=\"searchPanelNextButton\""));
    assertTrue(fxml.contains("fx:id=\"searchPanelClearButton\""));
  }
}
