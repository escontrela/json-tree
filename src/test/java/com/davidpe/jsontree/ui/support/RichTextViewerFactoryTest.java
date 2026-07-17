package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
