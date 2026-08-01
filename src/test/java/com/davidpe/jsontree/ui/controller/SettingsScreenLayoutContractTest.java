package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class SettingsScreenLayoutContractTest {

  @Test
  void settingsFxmlUsesConditionalVerticalScrollingAndExposesNightModeToggle()
      throws IOException {
    String fxml =
        new String(
            Objects.requireNonNull(
                    SettingsScreenLayoutContractTest.class.getResourceAsStream(
                        "/com/davidpe/jsontree/ui/settings.fxml"))
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertTrue(fxml.contains("<ScrollPane"));
    assertTrue(fxml.contains("fx:id=\"settingsScrollPane\""));
    assertTrue(fxml.contains("fitToWidth=\"true\""));
    assertTrue(fxml.contains("hbarPolicy=\"NEVER\""));
    assertTrue(fxml.contains("vbarPolicy=\"AS_NEEDED\""));
    assertTrue(fxml.contains("fx:id=\"nightModeCheckBox\""));
    assertTrue(fxml.contains("fx:id=\"shortcutsListBox\""));
    assertTrue(fxml.contains("Supported keyboard shortcuts"));
    assertTrue(fxml.contains("fx:id=\"settingsHeaderBackButton\""));
    assertTrue(fxml.contains("fx:id=\"settingsToolbarBackButton\""));
    assertTrue(fxml.contains("/com/davidpe/jsontree/images/back_35dp_000000.png"));
    assertTrue(fxml.contains("/com/davidpe/jsontree/images/back_35dp_FFFFFF.png"));
    assertTrue(!fxml.contains("text=\"&lt;&lt; Back\""));
    assertTrue(!fxml.contains("text=\"Back\""));
  }
}
