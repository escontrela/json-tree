package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonCropDocument;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.application.model.JsonSearchExecutionResult;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import com.davidpe.jsontree.application.model.RawJsonPresentation;
import com.davidpe.jsontree.application.service.JsonBreadcrumbModelService;
import com.davidpe.jsontree.application.service.JsonCropViewService;
import com.davidpe.jsontree.application.service.JsonOutlineModelService;
import com.davidpe.jsontree.application.service.RegexTextSearchService;
import com.davidpe.jsontree.application.service.RawJsonPresentationService;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.ui.controls.search.controller.SearchPanelController;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelCropState;
import com.davidpe.jsontree.ui.controls.search.support.SearchPanelDragSupport;
import com.davidpe.jsontree.ui.controls.search.support.SearchPanelPositioner;
import com.davidpe.jsontree.ui.controls.search.support.SearchPanelViewFactory;
import com.davidpe.jsontree.ui.controls.search.support.SearchPanelViewStateResolver;
import com.davidpe.jsontree.ui.controls.toolbar.ToolbarIconButton;
import com.davidpe.jsontree.ui.model.BreadcrumbViewerMode;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import com.davidpe.jsontree.ui.service.ZoomViewerStateBridge;
import com.davidpe.jsontree.ui.support.JsonBreadcrumbViewportResolver;
import com.davidpe.jsontree.ui.support.OutlineMinimapLayout;
import com.davidpe.jsontree.ui.support.OutlineMinimapLayoutPlanner;
import com.davidpe.jsontree.ui.support.OutlineMinimapRow;
import com.davidpe.jsontree.ui.support.OutlineMinimapScrollMapper;
import com.davidpe.jsontree.ui.support.OutlineViewportProjection;
import com.davidpe.jsontree.ui.support.OutlineViewportProjector;
import com.davidpe.jsontree.ui.support.RichTextViewerFactory;
import com.davidpe.jsontree.ui.support.RichTextViewerSurface;
import com.davidpe.jsontree.ui.support.SearchHighlightRange;
import com.davidpe.jsontree.ui.support.SearchMatchProjector;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlan;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlanFactory;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlanSearchOverlay;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Controller for the dedicated secondary zoom window shell.
 */
@Component
public class ZoomWindowController {

  private final RichTextViewerFactory richTextViewerFactory;
  private final ZoomViewerStateBridge zoomViewerStateBridge;
  private final RegexTextSearchService regexTextSearchService;
  private final JsonCropViewService jsonCropViewService;
  private final RawJsonPresentationService rawJsonPresentationService;
  private final JsonBreadcrumbModelService breadcrumbModelService;
  private final JsonOutlineModelService outlineModelService;
  private final SearchPanelViewFactory searchPanelViewFactory;
  private final SearchPanelViewStateResolver searchPanelViewStateResolver;
  private final SearchMatchProjector searchMatchProjector;
  private final ViewerTextRenderPlanFactory viewerTextRenderPlanFactory;
  private final ViewerTextRenderPlanSearchOverlay renderPlanSearchOverlay;
  private final JsonBreadcrumbViewportResolver breadcrumbViewportResolver;
  private final OutlineMinimapLayoutPlanner outlineLayoutPlanner;
  private final OutlineMinimapScrollMapper outlineScrollMapper;
  private final OutlineViewportProjector outlineViewportProjector;
  private final SearchPanelDragSupport searchPanelDragSupport =
      new SearchPanelDragSupport(new SearchPanelPositioner());

  private RichTextViewerSurface richTextViewerSurface;
  private Runnable zoomSubscriptionRelease;
  private ZoomViewerSnapshot currentSnapshot;
  private JsonSearchSession searchSession;
  private JsonCropDocument currentCropDocument;
  private JsonBreadcrumbModel currentCropBreadcrumbModel = JsonBreadcrumbModel.unavailable();
  private RawJsonPresentation currentRawPresentation = new RawJsonPresentation("", new int[] {0});
  private boolean cropActive;
  private String currentCropSourceIdentity;
  private String currentCropQuery;
  private boolean breadcrumbRefreshPending;
  private boolean outlineViewportRefreshPending;
  private OutlineMinimapLayout currentOutlineLayout = OutlineMinimapLayout.empty();
  private JsonOutlineModel currentOutlineModel = JsonOutlineModel.empty();
  private boolean zoomOutlineAutoClosedForPreview;
  private SearchPanelController searchPanelController;
  private String pendingSearchPanelQuery = "";
  private String pendingSearchPanelErrorText;
  private boolean searchNormalizedToRawMarkdown;

  @FXML private BorderPane rootPane;

  @FXML private Label zoomModeLabel;

  @FXML private Label zoomTitleLabel;

  @FXML private Label zoomMetaLabel;

  @FXML private Label zoomBreadcrumbLabel;

  @FXML private StackPane zoomViewerHost;

  @FXML private Label zoomStateLabel;

  @FXML private Pane zoomOverlayPane;

  @FXML private ToolbarIconButton zoomSearchButton;

  @FXML private Button zoomOutlineToggleButton;

  @FXML private VBox zoomOutlineVBox;

  @FXML private Label zoomOutlineTitleLabel;

  @FXML private StackPane zoomOutlinePreviewShell;

  @FXML private Canvas zoomOutlineCanvas;

  @FXML private Region zoomOutlineViewportMarker;

  @FXML private Label zoomOutlineStateLabel;

  @FXML private Label zoomOutlineMetaLabel;

  public ZoomWindowController(
      RichTextViewerFactory richTextViewerFactory,
      ZoomViewerStateBridge zoomViewerStateBridge,
      RegexTextSearchService regexTextSearchService,
      JsonCropViewService jsonCropViewService,
      RawJsonPresentationService rawJsonPresentationService,
      JsonBreadcrumbModelService breadcrumbModelService,
      JsonOutlineModelService outlineModelService,
      SearchPanelViewFactory searchPanelViewFactory,
      SearchPanelViewStateResolver searchPanelViewStateResolver,
      SearchMatchProjector searchMatchProjector,
      ViewerTextRenderPlanFactory viewerTextRenderPlanFactory,
      ViewerTextRenderPlanSearchOverlay renderPlanSearchOverlay,
      JsonBreadcrumbViewportResolver breadcrumbViewportResolver,
      OutlineMinimapLayoutPlanner outlineLayoutPlanner,
      OutlineMinimapScrollMapper outlineScrollMapper,
      OutlineViewportProjector outlineViewportProjector) {
    this.richTextViewerFactory = richTextViewerFactory;
    this.zoomViewerStateBridge = zoomViewerStateBridge;
    this.regexTextSearchService = regexTextSearchService;
    this.jsonCropViewService = jsonCropViewService;
    this.rawJsonPresentationService = rawJsonPresentationService;
    this.breadcrumbModelService = breadcrumbModelService;
    this.outlineModelService = outlineModelService;
    this.searchPanelViewFactory = searchPanelViewFactory;
    this.searchPanelViewStateResolver = searchPanelViewStateResolver;
    this.searchMatchProjector = searchMatchProjector;
    this.viewerTextRenderPlanFactory = viewerTextRenderPlanFactory;
    this.renderPlanSearchOverlay = renderPlanSearchOverlay;
    this.breadcrumbViewportResolver = breadcrumbViewportResolver;
    this.outlineLayoutPlanner = outlineLayoutPlanner;
    this.outlineScrollMapper = outlineScrollMapper;
    this.outlineViewportProjector = outlineViewportProjector;
  }

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    richTextViewerSurface = richTextViewerFactory.create();
    zoomViewerHost.getChildren().setAll(richTextViewerSurface.view());
    configureSearchPanel();
    richTextViewerSurface.addViewportChangeListener(
        () -> {
          scheduleBreadcrumbRefresh();
          scheduleOutlineViewportRefresh();
        });
    zoomOutlinePreviewShell.widthProperty().addListener((unused, oldValue, newValue) -> resizeOutlineCanvas());
    zoomOutlinePreviewShell.heightProperty().addListener((unused, oldValue, newValue) -> resizeOutlineCanvas());
    zoomOutlinePreviewShell.setOnMouseClicked(this::handleOutlineInteraction);
    refreshSearchPanelState(false);
    showAwaitingDocument();
  }

  private void configureSearchPanel() {
    if (searchPanelViewFactory == null) {
      return;
    }
    var searchPanelView = searchPanelViewFactory.create();
    searchPanelController = searchPanelView.controller();
    searchPanelController.bindHandlers(
        this::acceptSearchQuery,
        this::showPreviousSearchResult,
        this::showNextSearchResult,
        this::clearSearchSession,
        this::toggleCropView,
        this::hideSearchPanel);
    zoomOverlayPane.getChildren().setAll(searchPanelView.root());
    zoomOverlayPane.setManaged(false);
    zoomOverlayPane.setVisible(false);
    searchPanelDragSupport.attach(
        zoomOverlayPane, searchPanelController.root(), searchPanelController.dragHandle());
    searchPanelController.applyState(searchPanelViewStateResolver.hidden());
  }

  @FXML
  void closeWindow() {
    currentWindow().ifPresent(Window::hide);
  }

  @FXML
  void openSearchPanel() {
    if (!supportsSearch() || searchPanelController == null || zoomSearchButton.isDisable()) {
      return;
    }
    normalizeSearchPresentationIfNeeded();
    pendingSearchPanelErrorText = null;
    pendingSearchPanelQuery =
        searchSession != null ? searchSession.query() : searchPanelQueryText();
    refreshSearchPanelState(true);
    searchPanelController.revealAndFocus();
    zoomOverlayPane.setManaged(true);
    zoomOverlayPane.setVisible(true);
    searchPanelDragSupport.ensureInitialPosition();
  }

  void toggleCropView() {
    if (cropActive) {
      deactivateCropView();
      renderBaseSnapshot();
      return;
    }
    resolveCropDocument()
        .ifPresent(
            cropDocument -> {
              cropActive = true;
              currentCropDocument = cropDocument;
              currentCropSourceIdentity = snapshotSourceIdentity();
              currentCropQuery = searchSession == null ? null : searchSession.query();
              renderBaseSnapshot();
            });
  }

  void clearSearchSession() {
    deactivateCropView();
    searchSession = null;
    pendingSearchPanelErrorText = null;
    pendingSearchPanelQuery = "";
    refreshSearchPanelState(searchPanelController != null && searchPanelController.isShowing());
    renderBaseSnapshot();
    if (searchPanelController != null && searchPanelController.isShowing()) {
      searchPanelController.revealAndFocus();
    }
  }

  @FXML
  void showPreviousSearchResult() {
    moveSearchSelection(-1);
  }

  @FXML
  void showNextSearchResult() {
    moveSearchSelection(1);
  }

  private void acceptSearchQuery(String queryText) {
    if (!supportsSearch()) {
      return;
    }
    normalizeSearchPresentationIfNeeded();
    String previousQuery = searchSession == null ? null : searchSession.query();
    pendingSearchPanelQuery = queryText == null ? "" : queryText;
    JsonSearchExecutionResult result =
        regexTextSearchService.search(
            searchSourceIdentity(), pendingSearchPanelQuery, currentSearchSourceText());
    if (!result.successful()) {
      pendingSearchPanelErrorText = result.errorMessage();
      searchSession = null;
      refreshSearchPanelState(true);
      renderBaseSnapshot();
      return;
    }

    pendingSearchPanelErrorText = null;
    searchSession = result.session();
    if (!Objects.equals(previousQuery, searchSession.query())) {
      deactivateCropView();
    }
    pendingSearchPanelQuery = searchSession.query();
    refreshSearchPanelState(true);
    renderSearchAwareSnapshot();
  }

  private void hideSearchPanel() {
    if (searchPanelController == null) {
      return;
    }
    pendingSearchPanelQuery = searchPanelQueryText();
    searchPanelController.hidePanel();
    zoomOverlayPane.setManaged(false);
    zoomOverlayPane.setVisible(false);
  }

  @FXML
  void toggleZoomOutline() {
    if (zoomOutlineToggleButton.isDisable()) {
      return;
    }
    boolean nextVisible = !zoomOutlineVBox.isVisible();
    setZoomOutlineVisible(nextVisible);
    if (nextVisible) {
      resizeOutlineCanvas();
      scheduleOutlineViewportRefresh();
    }
  }

  public void showAwaitingDocument() {
    deactivateCropView();
    presentSnapshot(
        ZoomViewerSnapshot.empty(
            "JSON -> TREE • Zoom",
            "Zoom viewer",
            "Expanded reading surface",
            "Open a JSON in the main workspace to populate this reading surface."));
  }

  public void activate() {
    if (zoomSubscriptionRelease != null) {
      return;
    }
    zoomSubscriptionRelease = zoomViewerStateBridge.subscribe(this::presentSnapshot);
  }

  public void deactivate() {
    if (zoomSubscriptionRelease == null) {
      return;
    }
    zoomSubscriptionRelease.run();
    zoomSubscriptionRelease = null;
  }

  String viewerText() {
    return richTextViewerSurface.text();
  }

  String currentModeLabel() {
    return zoomModeLabel.getText();
  }

  boolean showingDocument() {
    return zoomViewerHost.isVisible();
  }

  boolean viewerEditable() {
    return richTextViewerSurface.editable();
  }

  private void presentSnapshot(ZoomViewerSnapshot snapshot) {
    if (snapshot == null) {
      showAwaitingDocument();
      return;
    }

    deactivateCropView();
    currentSnapshot = snapshot;
    searchNormalizedToRawMarkdown = false;
    currentOutlineModel = snapshot.outlineModel() == null ? JsonOutlineModel.empty() : snapshot.outlineModel();
    zoomModeLabel.setText(snapshot.modeLabel());
    zoomTitleLabel.setText(snapshot.documentTitle());
    applyMeta(snapshot.documentMeta());
    updateWindowTitle(snapshot.windowTitle());
    syncOutlineState(snapshot);
    syncSearchButtonState();

    if (snapshot.renderable() && snapshot.renderPlan() != null) {
      zoomViewerHost.setManaged(true);
      zoomViewerHost.setVisible(true);
      refreshSearchAgainstCurrentSnapshot();
      zoomStateLabel.setManaged(false);
      zoomStateLabel.setVisible(false);
      scheduleBreadcrumbRefresh();
      return;
    }

    searchSession = null;
    clearSearchPanelState(false);
    richTextViewerSurface.clear();
    richTextViewerSurface.hide();
    zoomViewerHost.setManaged(false);
    zoomViewerHost.setVisible(false);
    zoomStateLabel.setText(snapshot.emptyStateMessage());
    zoomStateLabel.setManaged(true);
    zoomStateLabel.setVisible(true);
    hideZoomBreadcrumb();
  }

  private void refreshSearchAgainstCurrentSnapshot() {
    syncSearchButtonState();
    String query = searchPanelQueryText().trim();
    if (query.isEmpty()) {
      searchSession = null;
      pendingSearchPanelErrorText = null;
      refreshSearchPanelState(searchPanelController != null && searchPanelController.isShowing());
      renderBaseSnapshot();
      return;
    }
    normalizeSearchPresentationIfNeeded();

    JsonSearchExecutionResult result =
        regexTextSearchService.search(searchSourceIdentity(), query, currentSearchSourceText());
    if (!result.successful()) {
      searchSession = null;
      pendingSearchPanelErrorText = result.errorMessage();
      refreshSearchPanelState(searchPanelController != null && searchPanelController.isShowing());
      renderBaseSnapshot();
      return;
    }

    pendingSearchPanelErrorText = null;
    searchSession = result.session();
    pendingSearchPanelQuery = searchSession.query();
    refreshSearchPanelState(searchPanelController != null && searchPanelController.isShowing());
    renderSearchAwareSnapshot();
  }

  private void renderBaseSnapshot() {
    if (currentSnapshot == null || currentSnapshot.renderPlan() == null) {
      return;
    }
    if (renderCropSnapshot()) {
      return;
    }
    currentOutlineModel =
        currentSnapshot.outlineModel() == null ? JsonOutlineModel.empty() : currentSnapshot.outlineModel();
    restoreBaseModeLabel();
    richTextViewerSurface.showStyledText(baseRenderPlan().fragments(), baseContentStyleClass());
    richTextViewerSurface.scrollToTop();
    scheduleBreadcrumbRefresh();
    scheduleOutlineViewportRefresh();
  }

  private void renderSearchAwareSnapshot() {
    if (currentSnapshot == null || currentSnapshot.renderPlan() == null) {
      return;
    }
    if (renderCropSnapshot()) {
      return;
    }
    currentOutlineModel =
        currentSnapshot.outlineModel() == null ? JsonOutlineModel.empty() : currentSnapshot.outlineModel();
    restoreBaseModeLabel();

    ViewerTextRenderPlan renderPlan =
        searchSession == null || !searchSession.hasMatches()
            ? baseRenderPlan()
            : renderPlanSearchOverlay.apply(baseRenderPlan(), currentHighlightRanges());
    richTextViewerSurface.showStyledText(renderPlan.fragments(), baseContentStyleClass());
    if (searchSession != null && searchSession.hasMatches()) {
      currentHighlightRanges().stream()
          .filter(SearchHighlightRange::active)
          .findFirst()
          .ifPresentOrElse(
              range -> richTextViewerSurface.scrollToOffset(range.startIndex()),
              richTextViewerSurface::scrollToTop);
    } else {
      richTextViewerSurface.scrollToTop();
    }
    scheduleBreadcrumbRefresh();
    scheduleOutlineViewportRefresh();
  }

  private void moveSearchSelection(int delta) {
    if (searchSession == null || !searchSession.hasMatches()) {
      return;
    }
    if (searchSession.totalMatches() > 1) {
      searchSession =
          searchSession.withActiveMatchIndex(
              Math.floorMod(searchSession.activeMatchIndex() + delta, searchSession.totalMatches()));
    }
    refreshSearchPanelState(searchPanelController != null && searchPanelController.isShowing());
    renderSearchAwareSnapshot();
  }

  private List<SearchHighlightRange> currentHighlightRanges() {
    if (searchSession == null || !searchSession.hasMatches()) {
      return List.of();
    }
    ViewerPresentationMode presentationMode = currentSearchPresentationMode();
    if (presentationMode == ViewerPresentationMode.RAW_JSON) {
      return searchMatchProjector.rawRanges(
          searchSession, currentRawPresentation.sourceToDisplayBoundaries());
    }
    if (presentationMode == ViewerPresentationMode.RAW_MARKDOWN) {
      return searchMatchProjector.rawRanges(searchSession);
    }
    return searchMatchProjector.asciiRanges(currentDisplayText(), searchSession);
  }

  private boolean renderCropSnapshot() {
    if (!cropActive) {
      return false;
    }
    if (!supportsCrop()) {
      deactivateCropView();
      return false;
    }

    return resolveCropDocument()
        .map(
            cropDocument -> {
              showCropSnapshot(cropDocument);
              return true;
            })
        .orElseGet(
            () -> {
              deactivateCropView();
              return false;
            });
  }

  private Optional<JsonCropDocument> resolveCropDocument() {
    if (!supportsCrop() || searchSession == null) {
      return Optional.empty();
    }

    String sourceIdentity = snapshotSourceIdentity();
    if (currentCropDocument != null
        && Objects.equals(currentCropSourceIdentity, sourceIdentity)
        && Objects.equals(currentCropQuery, searchSession.query())) {
      return Optional.of(currentCropDocument);
    }

    Optional<JsonCropDocument> cropDocument =
        jsonCropViewService.buildFromQuery(currentSnapshot.sourceRawText(), searchSession.query());
    currentCropDocument = cropDocument.orElse(null);
    currentCropSourceIdentity = cropDocument.isPresent() ? sourceIdentity : null;
    currentCropQuery = cropDocument.isPresent() ? searchSession.query() : null;
    return cropDocument;
  }

  private void showCropSnapshot(JsonCropDocument cropDocument) {
    currentCropBreadcrumbModel = breadcrumbModelService.buildFromRawJson(cropDocument.rawJson());
    currentOutlineModel = outlineModelService.buildFromRawJson(cropDocument.rawJson());
    zoomModeLabel.setText("Crop");
    if (currentSnapshot.presentationMode() == ViewerPresentationMode.RAW_JSON) {
      currentRawPresentation = rawJsonPresentationService.present(cropDocument.rawJson());
      ViewerTextRenderPlan renderPlan =
          viewerTextRenderPlanFactory.buildRawPlan(currentRawPresentation.content(), List.of());
      richTextViewerSurface.showStyledText(renderPlan.fragments(), "raw-json-content");
    } else {
      currentRawPresentation = new RawJsonPresentation("", new int[] {0});
      ViewerTextRenderPlan renderPlan =
          viewerTextRenderPlanFactory.buildAsciiPlan(cropDocument.asciiTreeDocument(), List.of());
      richTextViewerSurface.showStyledText(renderPlan.fragments(), "tree-content");
    }
    showOutlineValidShell();
    richTextViewerSurface.scrollToTop();
    scheduleBreadcrumbRefresh();
    scheduleOutlineViewportRefresh();
  }

  private boolean supportsCrop() {
    return currentSnapshot != null
        && currentSnapshot.renderable()
        && !currentSnapshot.largePreview()
        && !currentSearchPresentationMode().markdownMode()
        && currentSearchPresentationMode() != ViewerPresentationMode.STRUCTURE
        && !currentSnapshot.sourceRawText().isBlank()
        && searchSession != null
        && searchSession.hasMatches();
  }

  private void deactivateCropView() {
    cropActive = false;
    currentCropDocument = null;
    currentCropBreadcrumbModel = JsonBreadcrumbModel.unavailable();
    currentCropSourceIdentity = null;
    currentCropQuery = null;
    currentRawPresentation = new RawJsonPresentation("", new int[] {0});
  }

  private String snapshotSourceIdentity() {
    if (currentSnapshot == null) {
      return "zoom";
    }
    return currentSnapshot.windowTitle() + "::" + currentSnapshot.modeLabel();
  }

  private void restoreBaseModeLabel() {
    if (currentSnapshot != null) {
      zoomModeLabel.setText(currentSnapshot.modeLabel());
    }
  }

  private JsonBreadcrumbModel activeBreadcrumbModel() {
    if (cropActive) {
      return currentCropBreadcrumbModel;
    }
    return currentSnapshot == null ? JsonBreadcrumbModel.unavailable() : currentSnapshot.breadcrumbModel();
  }

  private String searchSourceIdentity() {
    if (currentSnapshot == null) {
      return "zoom";
    }
    return currentSnapshot.windowTitle() + "::" + currentSnapshot.modeLabel();
  }

  private void syncSearchButtonState() {
    if (zoomSearchButton != null) {
      zoomSearchButton.setDisable(!supportsSearch());
    }
  }

  private boolean supportsSearch() {
    return currentSnapshot != null
        && currentSnapshot.renderable()
        && !currentSnapshot.largePreview()
        && currentSnapshot.presentationMode() != ViewerPresentationMode.STRUCTURE
        && !currentSnapshot.sourceRawText().isBlank();
  }

  private void normalizeSearchPresentationIfNeeded() {
    if (currentSnapshot == null) {
      return;
    }
    searchNormalizedToRawMarkdown =
        currentSnapshot.presentationMode() == ViewerPresentationMode.MARKDOWN_RENDERED;
  }

  private ViewerPresentationMode currentSearchPresentationMode() {
    if (searchNormalizedToRawMarkdown) {
      return ViewerPresentationMode.RAW_MARKDOWN;
    }
    return currentSnapshot == null ? ViewerPresentationMode.ASCII_TREE : currentSnapshot.presentationMode();
  }

  private String currentSearchSourceText() {
    return currentSnapshot == null ? "" : currentSnapshot.sourceRawText();
  }

  private String searchPanelQueryText() {
    if (searchPanelController != null && searchPanelController.isShowing()) {
      return searchPanelController.queryText();
    }
    return pendingSearchPanelQuery == null ? "" : pendingSearchPanelQuery;
  }

  private void refreshSearchPanelState(boolean visible) {
    if (searchPanelController == null) {
      return;
    }
    if (!visible) {
      searchPanelController.applyState(searchPanelViewStateResolver.hidden());
      return;
    }
    if (pendingSearchPanelErrorText != null) {
      searchPanelController.applyState(
          searchPanelViewStateResolver.invalid(
              true,
              searchPanelQueryText(),
              pendingSearchPanelErrorText,
              currentSearchPanelCropState()));
      return;
    }
    if (searchSession != null) {
      searchPanelController.applyState(
          searchPanelViewStateResolver.active(true, searchSession, currentSearchPanelCropState()));
      return;
    }
    searchPanelController.applyState(
        searchPanelViewStateResolver.idle(
            true,
            searchPanelQueryText(),
            currentSearchIdleMessage(),
            currentSearchPanelCropState()));
  }

  private SearchPanelCropState currentSearchPanelCropState() {
    if (!supportsCrop() && !cropActive) {
      return SearchPanelCropState.hidden();
    }
    String affordanceText = cropActive ? "Return to full view" : "Show cropped view";
    return new SearchPanelCropState(true, supportsCrop() || cropActive, cropActive, affordanceText, affordanceText);
  }

  private String currentSearchIdleMessage() {
    if (currentSnapshot == null) {
      return "Java regular expression search. Literal fallback is disabled.";
    }
    if (currentSnapshot.presentationMode() == ViewerPresentationMode.MARKDOWN_RENDERED) {
      return "Regex search switches rendered Markdown into raw source mode so highlights stay source-aligned.";
    }
    if (currentSnapshot.presentationMode() == ViewerPresentationMode.RAW_MARKDOWN) {
      return "Regex search runs against the exact Markdown source. Literal fallback is disabled.";
    }
    return "Java regular expression search. Literal fallback is disabled.";
  }

  private void clearSearchPanelState(boolean visible) {
    searchSession = null;
    pendingSearchPanelErrorText = null;
    pendingSearchPanelQuery = "";
    if (!visible) {
      hideSearchPanel();
    }
    refreshSearchPanelState(visible);
  }

  private ViewerTextRenderPlan baseRenderPlan() {
    if (currentSnapshot == null || currentSnapshot.renderPlan() == null) {
      return ViewerTextRenderPlan.normal(List.of());
    }
    if (searchNormalizedToRawMarkdown) {
      currentRawPresentation = rawJsonPresentationService.presentPlainText(currentSnapshot.sourceRawText());
      return viewerTextRenderPlanFactory.buildRawMarkdownPlan(currentRawPresentation.content(), List.of());
    }
    if (currentSearchPresentationMode() == ViewerPresentationMode.RAW_JSON) {
      currentRawPresentation = rawJsonPresentationService.present(currentSnapshot.sourceRawText());
    }
    return currentSnapshot.renderPlan();
  }

  private String baseContentStyleClass() {
    return currentSearchPresentationMode() == ViewerPresentationMode.RAW_MARKDOWN
        ? "markdown-content"
        : currentSnapshot == null ? "" : currentSnapshot.contentStyleClass();
  }

  private String currentDisplayText() {
    ViewerTextRenderPlan basePlan = baseRenderPlan();
    return renderPlanSearchOverlay.flatten(basePlan);
  }

  private void scheduleBreadcrumbRefresh() {
    if (breadcrumbRefreshPending) {
      return;
    }
    breadcrumbRefreshPending = true;
    Platform.runLater(
        () -> {
          breadcrumbRefreshPending = false;
          refreshBreadcrumb();
        });
  }

  private void scheduleOutlineViewportRefresh() {
    if (outlineViewportRefreshPending) {
      return;
    }
    outlineViewportRefreshPending = true;
    Platform.runLater(
        () -> {
          outlineViewportRefreshPending = false;
          refreshOutlineViewportMarker();
        });
  }

  private void refreshBreadcrumb() {
    JsonBreadcrumbModel breadcrumbModel = activeBreadcrumbModel();
    if (currentSnapshot == null
        || !currentSnapshot.renderable()
        || currentSnapshot.largePreview()
        || breadcrumbModel == null
        || !breadcrumbModel.available()) {
      hideZoomBreadcrumb();
      return;
    }

    breadcrumbViewportResolver
        .resolve(
            breadcrumbModel,
            breadcrumbViewerMode(currentSnapshot.presentationMode()),
            richTextViewerSurface.firstVisibleParagraphIndex())
        .map(com.davidpe.jsontree.application.model.JsonBreadcrumbPath::displayLabel)
        .filter(text -> !text.isBlank())
        .ifPresentOrElse(this::showZoomBreadcrumb, this::hideZoomBreadcrumb);
  }

  private BreadcrumbViewerMode breadcrumbViewerMode(ViewerPresentationMode presentationMode) {
    if (presentationMode.rawTextMode()) {
      return BreadcrumbViewerMode.RAW_JSON;
    }
    if (presentationMode == ViewerPresentationMode.STRUCTURE) {
      return BreadcrumbViewerMode.STRUCTURE;
    }
    return BreadcrumbViewerMode.ASCII_TREE;
  }

  private void showZoomBreadcrumb(String breadcrumbText) {
    zoomBreadcrumbLabel.setText(breadcrumbText);
    zoomBreadcrumbLabel.setManaged(true);
    zoomBreadcrumbLabel.setVisible(true);
  }

  private void hideZoomBreadcrumb() {
    zoomBreadcrumbLabel.setText("");
    zoomBreadcrumbLabel.setManaged(false);
    zoomBreadcrumbLabel.setVisible(false);
  }

  private void applyMeta(String documentMeta) {
    boolean visible = documentMeta != null && !documentMeta.isBlank();
    zoomMetaLabel.setText(visible ? documentMeta : "");
    zoomMetaLabel.setManaged(visible);
    zoomMetaLabel.setVisible(visible);
  }

  private void updateWindowTitle(String windowTitle) {
    currentWindow()
        .filter(Stage.class::isInstance)
        .map(Stage.class::cast)
        .ifPresent(stage -> stage.setTitle(windowTitle));
  }

  private Optional<Window> currentWindow() {
    if (rootPane.getScene() == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(rootPane.getScene().getWindow());
  }

  private void syncOutlineState(ZoomViewerSnapshot snapshot) {
    if (snapshot == null || !snapshot.renderable()) {
      zoomOutlineToggleButton.setDisable(true);
      currentOutlineModel = JsonOutlineModel.empty();
      setZoomOutlineVisible(true);
      showOutlineShellState(
          "Awaiting document",
          "Open a JSON or Markdown file in the main workspace to populate the outline minimap shell.",
          "The panel keeps a dedicated minimap shell ready for the active document.",
          null);
      return;
    }

    if (snapshot.largePreview()) {
      zoomOutlineToggleButton.setDisable(true);
      zoomOutlineAutoClosedForPreview = true;
      setZoomOutlineVisible(false);
      currentOutlineModel = JsonOutlineModel.empty();
      showOutlineShellState(
          "Outline unavailable",
          "Large preview keeps the zoom reader focused on the active page chunk.",
          "Outline navigation stays disabled for preview mode in the zoom window.",
          "outline-state-loading");
      return;
    }

    boolean outlineAvailable = currentOutlineModel != null && !currentOutlineModel.emptyModel();
    zoomOutlineToggleButton.setDisable(!outlineAvailable);
    if (outlineAvailable) {
      if (zoomOutlineAutoClosedForPreview || !zoomOutlineVBox.isManaged()) {
        setZoomOutlineVisible(true);
      }
      zoomOutlineAutoClosedForPreview = false;
      showOutlineValidShell();
      return;
    }

    if (zoomOutlineAutoClosedForPreview || !zoomOutlineVBox.isManaged()) {
      setZoomOutlineVisible(true);
    }
    zoomOutlineAutoClosedForPreview = false;
    showOutlineShellState(
        "Outline unavailable",
        "The current zoom presentation does not expose a reusable outline model.",
        "Load a regular JSON or Markdown document in the main workspace to restore the minimap.",
        "outline-state-invalid");
  }

  private void showOutlineValidShell() {
    zoomOutlineTitleLabel.setText("Document outline");
    zoomOutlineMetaLabel.setText(
        currentOutlineModel.totalEntries()
            + " outline nodes • depth "
            + currentOutlineModel.maxDepth());
    zoomOutlineCanvas.setManaged(true);
    zoomOutlineCanvas.setVisible(true);
    zoomOutlineStateLabel.setManaged(false);
    zoomOutlineStateLabel.setVisible(false);
    zoomOutlineViewportMarker.setManaged(false);
    zoomOutlineViewportMarker.setVisible(false);
    zoomOutlinePreviewShell
        .getStyleClass()
        .removeAll("outline-state-loading", "outline-state-valid", "outline-state-invalid");
    zoomOutlinePreviewShell.getStyleClass().add("outline-state-valid");
    drawOutlineMinimap();
    scheduleOutlineViewportRefresh();
  }

  private void showOutlineShellState(
      String title, String stateMessage, String metaMessage, String previewStateClass) {
    zoomOutlineTitleLabel.setText(title);
    zoomOutlineMetaLabel.setText(metaMessage);
    zoomOutlineCanvas.setManaged(true);
    zoomOutlineCanvas.setVisible(true);
    zoomOutlineStateLabel.setText(stateMessage);
    zoomOutlineStateLabel.setManaged(true);
    zoomOutlineStateLabel.setVisible(true);
    zoomOutlineViewportMarker.setManaged(false);
    zoomOutlineViewportMarker.setVisible(false);
    zoomOutlinePreviewShell
        .getStyleClass()
        .removeAll("outline-state-loading", "outline-state-valid", "outline-state-invalid");
    if (previewStateClass != null) {
      zoomOutlinePreviewShell.getStyleClass().add(previewStateClass);
    }
    currentOutlineLayout = OutlineMinimapLayout.empty();
    drawOutlineShellPlaceholder();
  }

  private void resizeOutlineCanvas() {
    if (zoomOutlineCanvas == null || zoomOutlinePreviewShell == null) {
      return;
    }
    double width = Math.max(0.0, zoomOutlinePreviewShell.getWidth() - 2.0);
    double height = Math.max(0.0, zoomOutlinePreviewShell.getHeight() - 2.0);
    zoomOutlineCanvas.setWidth(width);
    zoomOutlineCanvas.setHeight(height);
    refreshOutlineCanvas();
  }

  private void refreshOutlineCanvas() {
    if (zoomOutlineStateLabel.isVisible()
        || currentOutlineModel.emptyModel()
        || !zoomOutlineCanvas.isVisible()) {
      currentOutlineLayout = OutlineMinimapLayout.empty();
      drawOutlineShellPlaceholder();
      hideOutlineViewportMarker();
      return;
    }
    drawOutlineMinimap();
    scheduleOutlineViewportRefresh();
  }

  private void drawOutlineMinimap() {
    currentOutlineLayout =
        outlineLayoutPlanner.plan(
            currentOutlineModel, zoomOutlineCanvas.getWidth(), zoomOutlineCanvas.getHeight());

    GraphicsContext graphics = zoomOutlineCanvas.getGraphicsContext2D();
    double width = zoomOutlineCanvas.getWidth();
    double height = zoomOutlineCanvas.getHeight();
    graphics.clearRect(0, 0, width, height);
    if (currentOutlineLayout.emptyLayout()) {
      return;
    }

    graphics.setFill(nightModeActive() ? Color.web("#18202a") : Color.web("#f6f8fb"));
    graphics.fillRect(0, 0, width, height);

    for (OutlineMinimapRow row : currentOutlineLayout.rows()) {
      graphics.setFill(outlineRowColor(row));
      graphics.fillRoundRect(row.x(), row.y(), row.width(), row.height(), 2.0, 2.0);
    }
  }

  private void drawOutlineShellPlaceholder() {
    GraphicsContext graphics = zoomOutlineCanvas.getGraphicsContext2D();
    double width = zoomOutlineCanvas.getWidth();
    double height = zoomOutlineCanvas.getHeight();
    graphics.clearRect(0, 0, width, height);
    if (width <= 0.0 || height <= 0.0) {
      return;
    }

    graphics.setFill(nightModeActive() ? Color.web("#18202a") : Color.web("#eef1f4"));
    graphics.fillRect(0, 0, width, height);

    double rowCount = 8.0;
    double rowHeight = Math.max(6.0, (height - 36.0) / rowCount);
    for (int index = 0; index < rowCount; index++) {
      double x = 14.0 + ((index % 3) * 10.0);
      double y = 18.0 + (index * rowHeight);
      double barWidth = Math.max(20.0, width - x - (18.0 + ((index % 4) * 6.0)));
      graphics.setFill(
          nightModeActive()
              ? (index % 2 == 0 ? Color.web("#324253") : Color.web("#2b3948"))
              : (index % 2 == 0 ? Color.web("#d5dbe3") : Color.web("#c8d1dc")));
      graphics.fillRect(x, y, barWidth, 3.0);
    }
  }

  private Color outlineRowColor(OutlineMinimapRow row) {
    if (nightModeActive()) {
      return switch (row.kind()) {
        case OBJECT -> Color.web("#7fb7ff");
        case ARRAY -> Color.web("#92a8c6");
        case VALUE -> Color.web("#60748c");
      };
    }
    return switch (row.kind()) {
      case OBJECT -> Color.web("#3569a3");
      case ARRAY -> Color.web("#6f8bad");
      case VALUE -> Color.web("#b5c0cd");
    };
  }

  private void handleOutlineInteraction(MouseEvent event) {
    if (zoomOutlineToggleButton.isDisable() || currentOutlineLayout.emptyLayout()) {
      return;
    }
    double scrollValue =
        outlineScrollMapper.scrollValueForPointer(
            event.getY(),
            zoomOutlinePreviewShell.getHeight(),
            richTextViewerSurface.viewportHeight(),
            richTextViewerSurface.totalContentHeightEstimate());
    richTextViewerSurface.scrollToVerticalValue(scrollValue);
    scheduleBreadcrumbRefresh();
    scheduleOutlineViewportRefresh();
    event.consume();
  }

  private void refreshOutlineViewportMarker() {
    if (currentOutlineLayout.emptyLayout() || !zoomOutlinePreviewShell.isVisible()) {
      hideOutlineViewportMarker();
      return;
    }

    OutlineViewportProjection projection =
        outlineViewportProjector.project(
            richTextViewerSurface.verticalScrollValue(),
            zoomOutlineCanvas.getHeight(),
            richTextViewerSurface.viewportHeight(),
            richTextViewerSurface.totalContentHeightEstimate());
    if (!projection.visible()) {
      hideOutlineViewportMarker();
      return;
    }

    double markerWidth = Math.max(24.0, zoomOutlineCanvas.getWidth() - 20.0);
    zoomOutlineViewportMarker.resizeRelocate(
        10.0, 1.0 + projection.y(), markerWidth, projection.height());
    zoomOutlineViewportMarker.setManaged(false);
    zoomOutlineViewportMarker.setVisible(true);
  }

  private void hideOutlineViewportMarker() {
    zoomOutlineViewportMarker.setManaged(false);
    zoomOutlineViewportMarker.setVisible(false);
  }

  private void setZoomOutlineVisible(boolean visible) {
    zoomOutlineVBox.setManaged(visible);
    zoomOutlineVBox.setVisible(visible);
    if (!visible) {
      hideOutlineViewportMarker();
    }
  }

  private boolean nightModeActive() {
    return rootPane.getStyleClass().contains("night-mode");
  }
}
