package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    assertTrue(fxml.contains("promptText=\"Regex search (Java Pattern)\""));
    assertTrue(fxml.contains("fx:id=\"searchPanelPreviousButton\""));
    assertTrue(fxml.contains("fx:id=\"searchPanelNextButton\""));
    assertTrue(fxml.contains("fx:id=\"searchPanelClearButton\""));
    assertTrue(fxml.contains("fx:id=\"searchPanelSubmitButton\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/previous_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/previous_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/next_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/next_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/clear_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/clear_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/close_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/close_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/filter_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/filter_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("tooltipText=\"Apply regex search\""));
    assertTrue(fxml.contains("tooltipText=\"Close search panel\""));
    assertTrue(fxml.contains("ToolbarIconButton"));
    assertTrue(!fxml.contains("arrow_back_ios_35dp_000000_FILL0_wght400_GRAD0_opsz40.png"));
    assertTrue(!fxml.contains("filter_alt_35dp_FFFFFF_FILL0_wght400_GRAD0_opsz40.png"));
    assertFalse(fxml.contains("text=\"Close\""));
    assertFalse(fxml.contains("text=\"Prev\""));
    assertFalse(fxml.contains("text=\"Next\""));
    assertFalse(fxml.contains("text=\"Clear\""));
    assertFalse(fxml.contains("text=\"Apply\""));
  }
}
