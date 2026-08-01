package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.application.service.BestEffortJsonPrettyPrinter;
import com.davidpe.jsontree.application.service.JsonBreadcrumbModelService;
import com.davidpe.jsontree.application.service.JsonCropViewService;
import com.davidpe.jsontree.application.service.JsonOutlineModelService;
import com.davidpe.jsontree.application.service.RegexTextSearchService;
import com.davidpe.jsontree.application.service.RawJsonPresentationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.davidpe.jsontree.ui.controls.search.controller.SearchPanelController;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelView;
import com.davidpe.jsontree.ui.controls.search.support.SearchPanelViewFactory;
import com.davidpe.jsontree.ui.controls.search.support.SearchPanelViewStateResolver;
import com.davidpe.jsontree.ui.controls.toolbar.ToolbarIconButton;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import com.davidpe.jsontree.ui.service.ZoomViewerStateBridge;
import com.davidpe.jsontree.ui.support.JavaFxThreadTestSupport;
import com.davidpe.jsontree.ui.support.JsonBreadcrumbViewportResolver;
import com.davidpe.jsontree.ui.support.OutlineMinimapLayoutPlanner;
import com.davidpe.jsontree.ui.support.OutlineMinimapScrollMapper;
import com.davidpe.jsontree.ui.support.OutlineViewportProjector;
import com.davidpe.jsontree.ui.support.AsciiTreeSyntaxHighlighter;
import com.davidpe.jsontree.ui.support.MarkdownTextSyntaxHighlighter;
import com.davidpe.jsontree.ui.support.RenderedMarkdownTextRenderer;
import com.davidpe.jsontree.ui.support.RichTextViewerFactory;
import com.davidpe.jsontree.ui.support.RichTextViewerSurface;
import com.davidpe.jsontree.ui.support.SearchTextSpanHighlighter;
import com.davidpe.jsontree.ui.support.SearchMatchProjector;
import com.davidpe.jsontree.ui.support.SearchHighlightRangeNormalizer;
import com.davidpe.jsontree.ui.support.ViewerTextRenderFragment;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlan;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlanFactory;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlanSearchOverlay;
import java.lang.reflect.Field;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

          controller.openSearchPanel();
          SearchPanelController searchPanel = (SearchPanelController) getField(controller, "searchPanelController");
          TextField searchField = (TextField) getField(searchPanel, "searchPanelQueryField");
          Label occurrenceLabel = (Label) getField(searchPanel, "searchPanelOccurrenceLabel");
          ToolbarIconButton previousButton =
              (ToolbarIconButton) getField(searchPanel, "searchPanelPreviousButton");
          ToolbarIconButton nextButton =
              (ToolbarIconButton) getField(searchPanel, "searchPanelNextButton");
          Label helperLabel = (Label) getField(searchPanel, "searchPanelHelperLabel");

          searchField.setText("alpha");
          invokeMethod(searchPanel, "submitSearch");

          assertEquals("1 of 2", occurrenceLabel.getText());
          assertFalse(previousButton.isDisable());
          assertFalse(nextButton.isDisable());
          assertTrue(helperLabel.getText().contains("Search session active"));
          assertTrue(richTextSurface(controller).styleClassesAt(0).contains("search-match-active"));

          controller.showNextSearchResult();
          assertEquals("2 of 2", occurrenceLabel.getText());

          searchField.setText("");
          invokeMethod(searchPanel, "submitSearch");
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

          controller.openSearchPanel();
          SearchPanelController searchPanel = (SearchPanelController) getField(controller, "searchPanelController");
          TextField searchField = (TextField) getField(searchPanel, "searchPanelQueryField");
          Label helperLabel = (Label) getField(searchPanel, "searchPanelHelperLabel");

          searchField.setText("[");
          invokeMethod(searchPanel, "submitSearch");

          assertTrue(helperLabel.getText().contains("Unclosed"));
          assertEquals("alpha beta alpha", controller.viewerText());
        });
  }

  @Test
  void searchesAgainstSourceAlignedTextInsteadOfRenderedZoomText() {
    JavaFxThreadTestSupport.runOnFxThread(
        () -> {
          ZoomViewerStateBridge bridge = new ZoomViewerStateBridge();
          ZoomWindowController controller = controller(bridge);

          controller.initialize();
          controller.activate();
          bridge.publish(
              ZoomViewerSnapshot.renderable(
                  false,
                  "JSON -> TREE • Zoom • doc.md",
                  "Markdown",
                  "doc.md",
                  "1.0 KB • markdown",
                  "reference\n- item",
                  ViewerTextRenderPlan.normal(
                      List.of(
                          new ViewerTextRenderFragment(
                              "reference item", "markdown-paragraph", "#2d333a", false, false))),
                  "markdown-content",
                  ViewerPresentationMode.MARKDOWN_RENDERED,
                  JsonBreadcrumbModel.unavailable(),
                  JsonOutlineModel.empty()));

          controller.openSearchPanel();
          SearchPanelController searchPanel = (SearchPanelController) getField(controller, "searchPanelController");
          TextField searchField = (TextField) getField(searchPanel, "searchPanelQueryField");
          Label occurrenceLabel = (Label) getField(searchPanel, "searchPanelOccurrenceLabel");

          searchField.setText("reference.*item");
          invokeMethod(searchPanel, "submitSearch");

          assertEquals("0 matches", occurrenceLabel.getText());
        });
  }

  private ZoomWindowController controller(ZoomViewerStateBridge bridge) {
    ObjectMapper objectMapper = new ObjectMapper();
    RawJsonPresentationService rawJsonPresentationService =
        new RawJsonPresentationService(objectMapper, new BestEffortJsonPrettyPrinter());
    SearchPanelViewFactory searchPanelViewFactory = new SearchPanelViewFactory(null) {
      @Override
      public SearchPanelView create() {
        SearchPanelController controller = new SearchPanelController();
        setField(controller, "searchPanelRoot", new VBox());
        setField(controller, "searchPanelDragHandle", new HBox());
        setField(controller, "searchPanelOccurrenceLabel", new Label());
        setField(controller, "searchPanelQueryField", new TextField());
        setField(controller, "searchPanelHelperLabel", new Label());
        setField(controller, "searchPanelSubmitButton", new ToolbarIconButton());
        setField(controller, "searchPanelPreviousButton", new ToolbarIconButton());
        setField(controller, "searchPanelNextButton", new ToolbarIconButton());
        setField(controller, "searchPanelClearButton", new ToolbarIconButton());
        setField(controller, "searchPanelCropButton", new ToolbarIconButton());
        controller.initialize();
        return new SearchPanelView((VBox) getField(controller, "searchPanelRoot"), controller);
      }
    };
    ZoomWindowController controller =
        new ZoomWindowController(
            new RichTextViewerFactory(),
            bridge,
            new RegexTextSearchService(),
            new JsonCropViewService(
                objectMapper,
                new com.davidpe.jsontree.application.service.JsonSemanticSearchPathResolverService(objectMapper),
                new com.davidpe.jsontree.infrastructure.rendering.JacksonAsciiTreeFormatter(objectMapper)),
            rawJsonPresentationService,
            new JsonBreadcrumbModelService(objectMapper, rawJsonPresentationService),
            new JsonOutlineModelService(objectMapper),
            searchPanelViewFactory,
            new SearchPanelViewStateResolver(),
            new SearchMatchProjector(),
            new ViewerTextRenderPlanFactory(
                new AsciiTreeSyntaxHighlighter(),
                new SearchTextSpanHighlighter(),
                new MarkdownTextSyntaxHighlighter(),
                new RenderedMarkdownTextRenderer(),
                new ViewerTextRenderPlanSearchOverlay(new SearchHighlightRangeNormalizer())),
            new ViewerTextRenderPlanSearchOverlay(new SearchHighlightRangeNormalizer()),
            new JsonBreadcrumbViewportResolver(),
            new OutlineMinimapLayoutPlanner(),
            new OutlineMinimapScrollMapper(),
            new OutlineViewportProjector());
    setField(controller, "rootPane", new BorderPane());
    setField(controller, "zoomModeLabel", new Label());
    setField(controller, "zoomTitleLabel", new Label());
    setField(controller, "zoomMetaLabel", new Label());
    setField(controller, "zoomBreadcrumbLabel", new Label());
    setField(controller, "zoomViewerHost", new StackPane());
    setField(controller, "zoomStateLabel", new Label());
    setField(controller, "zoomOverlayPane", new Pane());
    setField(controller, "zoomSearchButton", new ToolbarIconButton());
    setField(controller, "zoomOutlineToggleButton", new ToolbarIconButton());
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
        "{\"value\":\"alpha beta alpha\"}",
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

  private static void invokeMethod(Object target, String methodName) {
    try {
      var method = target.getClass().getDeclaredMethod(methodName);
      method.setAccessible(true);
      method.invoke(target);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }
}
