package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.application.service.RegexTextSearchService;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import com.davidpe.jsontree.ui.service.ZoomViewerStateBridge;
import com.davidpe.jsontree.ui.support.JavaFxThreadTestSupport;
import com.davidpe.jsontree.ui.support.JsonBreadcrumbViewportResolver;
import com.davidpe.jsontree.ui.support.OutlineMinimapLayoutPlanner;
import com.davidpe.jsontree.ui.support.OutlineMinimapScrollMapper;
import com.davidpe.jsontree.ui.support.OutlineViewportProjector;
import com.davidpe.jsontree.ui.support.RichTextViewerFactory;
import com.davidpe.jsontree.ui.support.RichTextViewerSurface;
import com.davidpe.jsontree.ui.support.SearchHighlightRangeNormalizer;
import com.davidpe.jsontree.ui.support.ViewerTextRenderFragment;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlan;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlanSearchOverlay;
import java.lang.reflect.Field;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

class ZoomWindowControllerSearchTest {

  @Test
  void searchesNavigatesAndClearsAgainstTheCurrentZoomSnapshot() {
    JavaFxThreadTestSupport.runOnFxThread(
        () -> {
          ZoomViewerStateBridge bridge = new ZoomViewerStateBridge();
          ZoomWindowController controller = controller(bridge);

          controller.initialize();
          controller.activate();
          bridge.publish(renderableSnapshot("alpha beta alpha"));

          TextField searchField = (TextField) getField(controller, "zoomSearchField");
          Label occurrenceLabel = (Label) getField(controller, "zoomSearchOccurrenceLabel");
          Button previousButton = (Button) getField(controller, "zoomSearchPreviousButton");
          Button nextButton = (Button) getField(controller, "zoomSearchNextButton");
          Label errorLabel = (Label) getField(controller, "zoomSearchErrorLabel");

          searchField.setText("alpha");
          controller.executeSearch();

          assertEquals("1 of 2", occurrenceLabel.getText());
          assertFalse(previousButton.isDisable());
          assertFalse(nextButton.isDisable());
          assertFalse(errorLabel.isVisible());
          assertTrue(richTextSurface(controller).styleClassesAt(0).contains("search-match-active"));

          controller.showNextSearchResult();
          assertEquals("2 of 2", occurrenceLabel.getText());

          searchField.setText("");
          controller.executeSearch();
          assertEquals("Ready", occurrenceLabel.getText());
          assertTrue(previousButton.isDisable());
          assertTrue(nextButton.isDisable());
        });
  }

  @Test
  void showsAReadableErrorForInvalidRegexWithoutBreakingTheViewer() {
    JavaFxThreadTestSupport.runOnFxThread(
        () -> {
          ZoomViewerStateBridge bridge = new ZoomViewerStateBridge();
          ZoomWindowController controller = controller(bridge);

          controller.initialize();
          controller.activate();
          bridge.publish(renderableSnapshot("alpha beta alpha"));

          TextField searchField = (TextField) getField(controller, "zoomSearchField");
          Label errorLabel = (Label) getField(controller, "zoomSearchErrorLabel");

          searchField.setText("[");
          controller.executeSearch();

          assertTrue(errorLabel.isVisible());
          assertFalse(errorLabel.getText().isBlank());
          assertEquals("alpha beta alpha", controller.viewerText());
        });
  }

  private ZoomWindowController controller(ZoomViewerStateBridge bridge) {
    ZoomWindowController controller =
        new ZoomWindowController(
            new RichTextViewerFactory(),
            bridge,
            new RegexTextSearchService(),
            new ViewerTextRenderPlanSearchOverlay(new SearchHighlightRangeNormalizer()),
            new JsonBreadcrumbViewportResolver(),
            new OutlineMinimapLayoutPlanner(),
            new OutlineMinimapScrollMapper(),
            new OutlineViewportProjector());
    setField(controller, "rootPane", new BorderPane());
    setField(controller, "zoomModeLabel", new Label());
    setField(controller, "zoomTitleLabel", new Label());
    setField(controller, "zoomMetaLabel", new Label());
    setField(controller, "zoomSearchField", new TextField());
    setField(controller, "zoomSearchPreviousButton", new Button("Previous"));
    setField(controller, "zoomSearchNextButton", new Button("Next"));
    setField(controller, "zoomSearchOccurrenceLabel", new Label());
    setField(controller, "zoomSearchErrorLabel", new Label());
    setField(controller, "zoomBreadcrumbLabel", new Label());
    setField(controller, "zoomViewerHost", new StackPane());
    setField(controller, "zoomStateLabel", new Label());
    setField(controller, "zoomOutlineToggleButton", new Button("Outline"));
    setField(controller, "zoomOutlineVBox", new javafx.scene.layout.VBox());
    setField(controller, "zoomOutlineTitleLabel", new Label());
    setField(controller, "zoomOutlinePreviewShell", new StackPane());
    setField(controller, "zoomOutlineCanvas", new javafx.scene.canvas.Canvas());
    setField(controller, "zoomOutlineViewportMarker", new javafx.scene.layout.Region());
    setField(controller, "zoomOutlineStateLabel", new Label());
    setField(controller, "zoomOutlineMetaLabel", new Label());
    return controller;
  }

  private ZoomViewerSnapshot renderableSnapshot(String text) {
    return ZoomViewerSnapshot.renderable(
        false,
        "JSON -> TREE • Zoom • sample.json",
        "ASCII tree",
        "sample.json",
        "1.0 KB • local import",
        ViewerTextRenderPlan.normal(
            List.of(new ViewerTextRenderFragment(text, "tree-default", "#2d333a", false, false))),
        "tree-content",
        ViewerPresentationMode.ASCII_TREE,
        JsonBreadcrumbModel.unavailable(),
        JsonOutlineModel.empty());
  }

  private RichTextViewerSurface richTextSurface(ZoomWindowController controller) {
    return (RichTextViewerSurface) getField(controller, "richTextViewerSurface");
  }

  private static Object getField(Object target, String fieldName) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }
}
