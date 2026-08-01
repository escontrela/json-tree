package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MainWindowToolbarCleanupTest {

  @Test
  void keepsTheMainViewerActionsOnTheSharedIconToolbar() throws IOException {
    String fxml =
        new String(
            MainWindowToolbarCleanupTest.class
                .getResourceAsStream("/com/davidpe/jsontree/ui/main.fxml")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    assertFalse(fxml.contains("text=\"Focus\""));
    assertFalse(fxml.contains("text=\"Density\""));
    assertFalse(fxml.contains("text=\"Copy tree\""));
    assertFalse(fxml.contains("text=\"Raw JSON\""));
    assertFalse(fxml.contains("text=\"Search\""));
    assertFalse(fxml.contains("text=\"Structure\""));
    assertFalse(fxml.contains("text=\"Crop\""));
    assertFalse(fxml.contains("text=\"Settings\""));
    int copyIndex = fxml.indexOf("fx:id=\"copyTreeButton\"");
    int rawIndex = fxml.indexOf("fx:id=\"rawJsonButton\"");
    int searchIndex = fxml.indexOf("fx:id=\"searchButton\"");
    int structureIndex = fxml.indexOf("fx:id=\"structureButton\"");
    int fontSizeIndex = fxml.indexOf("fx:id=\"fontSizeButton\"");
    int zoomIndex = fxml.indexOf("fx:id=\"zoomButton\"");
    int outlineIndex = fxml.indexOf("fx:id=\"outlineToggleButton\"");

    assertTrue(copyIndex >= 0);
    assertTrue(rawIndex > copyIndex);
    assertTrue(searchIndex > rawIndex);
    assertTrue(structureIndex > searchIndex);
    assertTrue(fontSizeIndex > structureIndex);
    assertTrue(zoomIndex > fontSizeIndex);
    assertTrue(outlineIndex > zoomIndex);
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/copy_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/copy_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/raw_on_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/raw_on_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/search_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/search_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/structure_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/structure_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/font_size_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/font_size_35dp_FFFFFF.png\""));
    assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/outline_35dp_000000.png\""));
    assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/outline_35dp_FFFFFFF.png\""));
    assertTrue(fxml.contains("toggleMode=\"true\""));
    assertTrue(fxml.contains("tooltipText=\"Copy tree\""));
    assertTrue(fxml.contains("tooltipText=\"Show raw view\""));
    assertTrue(fxml.contains("tooltipText=\"Open search panel\""));
    assertTrue(fxml.contains("tooltipText=\"Show structure view\""));
    assertTrue(fxml.contains("tooltipText=\"Viewer text size: default\""));
    assertTrue(fxml.contains("tooltipText=\"Toggle outline panel\""));
    assertTrue(fxml.contains("tooltipText=\"Open zoom viewer\""));
    assertFalse(fxml.contains("fx:id=\"cropButton\""));
  }
}
