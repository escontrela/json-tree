package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RichTextViewerFactoryTest {

  @Test
  void createsReadOnlyViewerSurfaceBackedByRichTextFx() {
    JavaFxThreadTestSupport.runOnFxThread(
        () -> {
          RichTextViewerSurface surface = new RichTextViewerFactory().create();

          assertNotNull(surface.view());
          assertFalse(surface.editable());
          assertFalse(surface.view().isVisible());

          surface.replaceText("root\n└─ id: 1");
          surface.show();

          assertTrue(surface.view().isVisible());
          assertTrue(surface.text().contains("id: 1"));
        });
  }

  @Test
  void exposesRichTextFxDependencyAtRuntime() throws Exception {
    JavaFxThreadTestSupport.runOnFxThread(
        () -> assertNotNull(new RichTextViewerFactory().create().view()));
  }

  @Test
  void reusesSingleViewerSurfaceAcrossAsciiAndRawModes() {
    JavaFxThreadTestSupport.runOnFxThread(
        () -> {
          RichTextViewerSurface surface = new RichTextViewerFactory().create();

          surface.showText("root\n└─ id: 1", "tree-content");
          assertTrue(surface.hasContentStyleClass("tree-content"));
          assertFalse(surface.hasContentStyleClass("raw-json-content"));
          assertTrue(surface.text().contains("id: 1"));

          surface.showText("{\"id\":1}", "raw-json-content");
          assertTrue(surface.hasContentStyleClass("raw-json-content"));
          assertFalse(surface.hasContentStyleClass("tree-content"));
          assertTrue(surface.text().contains("\"id\""));

          surface.hide();
          assertFalse(surface.view().isVisible());
        });
  }

  @Test
  void appliesStyleSpansToRichTextContent() {
    JavaFxThreadTestSupport.runOnFxThread(
        () -> {
          RichTextViewerSurface surface = new RichTextViewerFactory().create();

          surface.showStyledText(
              List.of(
                  new ViewerTextRenderFragment("root", "tree-structure", "#1a2129", false, false),
                  new ViewerTextRenderFragment("\n", "tree-default", "#2d333a", false, false),
                  new ViewerTextRenderFragment("name", "tree-key", "#2d333a", true, true)),
              "tree-content");

          assertIterableEquals(List.of("tree-structure"), surface.styleClassesAt(0));
          assertIterableEquals(
              List.of("tree-key", "search-match", "search-match-active"),
              surface.styleClassesAt(5));
        });
  }
}
