package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class MainWindowInlineHistoryLayoutTest {

  @Test
  void mainFxmlUsesHistoryIconForTheInlineArchiveEntryPoint() throws IOException {
    String fxml =
        new String(
            Objects.requireNonNull(
                    MainWindowInlineHistoryLayoutTest.class.getResourceAsStream(
                        "/com/davidpe/jsontree/ui/main.fxml"))
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertTrue(fxml.contains("fx:id=\"inlineHistoryOpenButton\""));
    assertTrue(fxml.contains("onAction=\"#openHistory\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/history_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/history_35dp_FFFFFF.png\""));
    assertFalse(fxml.contains("text=\"View all\""));
  }
}
