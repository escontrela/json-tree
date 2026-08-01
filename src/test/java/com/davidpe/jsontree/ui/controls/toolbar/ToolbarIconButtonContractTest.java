package com.davidpe.jsontree.ui.controls.toolbar;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ToolbarIconButtonContractTest {

  @Test
  void hostFxmlEmbedsTheReusableToolbarIconButtonWithThemeAwareResources() throws IOException {
    String fxml = readResource("/com/davidpe/jsontree/ui/controls/toolbar-icon-button-host.fxml");

    assertTrue(fxml.contains("ToolbarIconButton"));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/zoom_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/zoom_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("tooltipText=\"Zoom viewer\""));
    assertTrue(fxml.contains("accessibleText=\"Zoom viewer\""));
    assertTrue(fxml.contains("disable=\"true\""));
  }

  @Test
  void referencedToolbarAssetsExistOnTheClasspath() {
    assertNotNull(
        ToolbarIconButtonContractTest.class.getResource(
            "/com/davidpe/jsontree/images/zoom_35dp_000000.png"));
    assertNotNull(
        ToolbarIconButtonContractTest.class.getResource(
            "/com/davidpe/jsontree/images/zoom_35dp_FFFFFF.png"));
  }

  private static String readResource(String resourcePath) throws IOException {
    try (InputStream stream = ToolbarIconButtonContractTest.class.getResourceAsStream(resourcePath)) {
      assertNotNull(stream, "Resource not found: " + resourcePath);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
