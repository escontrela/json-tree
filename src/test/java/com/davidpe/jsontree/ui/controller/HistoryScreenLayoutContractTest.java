package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class HistoryScreenLayoutContractTest {

  @Test
  void historyFxmlUsesIconButtonsForBackAndSearch() throws IOException {
    String fxml =
        new String(
            Objects.requireNonNull(
                    HistoryScreenLayoutContractTest.class.getResourceAsStream(
                        "/com/davidpe/jsontree/ui/history.fxml"))
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertTrue(fxml.contains("fx:id=\"historyBackButton\""));
    assertTrue(fxml.contains("fx:id=\"historySearchButton\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/close_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/close_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/search_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/search_35dp_FFFFFF.png\""));
    assertFalse(fxml.contains("text=\"Back\""));
    assertFalse(fxml.contains("text=\"Search\""));
  }
}
