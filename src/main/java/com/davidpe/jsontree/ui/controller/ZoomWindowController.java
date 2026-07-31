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
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
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
  private final ViewerTextRenderPlanFactory viewerTextRenderPlanFactory;
  private final ViewerTextRenderPlanSearchOverlay renderPlanSearchOverlay;
  private final JsonBreadcrumbViewportResolver breadcrumbViewportResolver;
  private final OutlineMinimapLayoutPlanner outlineLayoutPlanner;
  private final OutlineMinimapScrollMapper outlineScrollMapper;
  private final OutlineViewportProjector outlineViewportProjector;

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

  @FXML private BorderPane rootPane;

  @FXML private Label zoomModeLabel;

  @FXML private Label zoomTitleLabel;

  @FXML private Label zoomMetaLabel;

  @FXML private TextField zoomSearchField;

  @FXML private Button zoomCropButton;

  @FXML private Button zoomSearchPreviousButton;

  @FXML private Button zoomSearchNextButton;

  @FXML private Label zoomSearchOccurrenceLabel;

  @FXML private Label zoomSearchErrorLabel;

  @FXML private Label zoomBreadcrumbLabel;

  @FXML private StackPane zoomViewerHost;

  @FXML private Label zoomStateLabel;

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
    richTextViewerSurface.addViewportChangeListener(
        () -> {
          scheduleBreadcrumbRefresh();
          scheduleOutlineViewportRefresh();
        });
    zoomOutlinePreviewShell.widthProperty().addListener((unused, oldValue, newValue) -> resizeOutlineCanvas());
    zoomOutlinePreviewShell.heightProperty().addListener((unused, oldValue, newValue) -> resizeOutlineCanvas());
    zoomOutlinePreviewShell.setOnMouseClicked(this::handleOutlineInteraction);
    syncSearchControls();
    syncCropButtonState();
    showAwaitingDocument();
  }

  @FXML
  void closeWindow() {
    currentWindow().ifPresent(Window::hide);
  }

  @FXML
  void executeSearch() {
    String previousQuery = searchSession == null ? null : searchSession.query();
    String query = zoomSearchField.getText();
    if (query == null || query.trim().isEmpty()) {
      deactivateCropView();
      clearSearchState();
      return;
    }
    if (currentSnapshot == null || !currentSnapshot.renderable() || currentSnapshot.renderPlan() == null) {
      showSearchError("No text is available for search.");
      return;
    }

    JsonSearchExecutionResult result =
        regexTextSearchService.search(searchSourceIdentity(), query, currentRenderedText());
    if (!result.successful()) {
      showSearchError(result.errorMessage());
      return;
    }

    hideSearchError();
    searchSession = result.session();
    if (!Objects.equals(previousQuery, searchSession.query())) {
      deactivateCropView();
    }
    syncSearchControls();
    renderSearchAwareSnapshot();
  }

  @FXML
  void toggleCropView() {
    if (zoomCropButton.isDisable()) {
      return;
    }
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

  @FXML
  void showPreviousSearchResult() {
    moveSearchSelection(-1);
  }

  @FXML
  void showNextSearchResult() {
    moveSearchSelection(1);
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
    currentOutlineModel = snapshot.outlineModel() == null ? JsonOutlineModel.empty() : snapshot.outlineModel();
    zoomModeLabel.setText(snapshot.modeLabel());
    zoomTitleLabel.setText(snapshot.documentTitle());
    applyMeta(snapshot.documentMeta());
    updateWindowTitle(snapshot.windowTitle());
    syncOutlineState(snapshot);

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
    syncSearchControls();
    hideSearchError();
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
    String query = zoomSearchField.getText() == null ? "" : zoomSearchField.getText().trim();
    if (query.isEmpty()) {
      searchSession = null;
      syncSearchControls();
      renderBaseSnapshot();
      return;
    }

    JsonSearchExecutionResult result =
        regexTextSearchService.search(searchSourceIdentity(), query, currentRenderedText());
    if (!result.successful()) {
      searchSession = null;
      syncSearchControls();
      showSearchError(result.errorMessage());
      renderBaseSnapshot();
      return;
    }

    hideSearchError();
    searchSession = result.session();
    syncSearchControls();
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
    richTextViewerSurface.showStyledText(
        currentSnapshot.renderPlan().fragments(), currentSnapshot.contentStyleClass());
    richTextViewerSurface.scrollToTop();
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
            ? currentSnapshot.renderPlan()
            : renderPlanSearchOverlay.apply(currentSnapshot.renderPlan(), currentHighlightRanges());
    richTextViewerSurface.showStyledText(renderPlan.fragments(), currentSnapshot.contentStyleClass());
    if (searchSession != null) {
      searchSession
          .activeMatch()
          .ifPresentOrElse(
              match -> richTextViewerSurface.scrollToOffset(match.startIndex()),
              richTextViewerSurface::scrollToTop);
    } else {
      richTextViewerSurface.scrollToTop();
    }
    scheduleBreadcrumbRefresh();
    scheduleOutlineViewportRefresh();
  }

  private String currentRenderedText() {
    return currentSnapshot == null || currentSnapshot.renderPlan() == null
        ? ""
        : renderPlanSearchOverlay.flatten(currentSnapshot.renderPlan());
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
    syncSearchControls();
    renderSearchAwareSnapshot();
  }

  private List<SearchHighlightRange> currentHighlightRanges() {
    if (searchSession == null || !searchSession.hasMatches()) {
      return List.of();
    }

    Optional<com.davidpe.jsontree.application.model.JsonSearchMatch> activeMatch =
        searchSession.activeMatch();
    return searchSession.matches().stream()
        .map(
            match ->
                new SearchHighlightRange(
                    match.startIndex(),
                    match.endIndex(),
                    activeMatch.map(match::equals).orElse(false)))
        .toList();
  }

  private void clearSearchState() {
    deactivateCropView();
    searchSession = null;
    hideSearchError();
    syncSearchControls();
    renderBaseSnapshot();
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
        && !currentSnapshot.presentationMode().markdownMode()
        && currentSnapshot.presentationMode() != ViewerPresentationMode.STRUCTURE
        && !currentSnapshot.sourceRawText().isBlank()
        && searchSession != null
        && searchSession.hasMatches();
  }

  private void syncCropButtonState() {
    boolean available = supportsCrop();
    zoomCropButton.setDisable(!(available || cropActive));
    zoomCropButton.setText(cropActive ? "Full view" : "Crop");
  }

  private void deactivateCropView() {
    cropActive = false;
    currentCropDocument = null;
    currentCropBreadcrumbModel = JsonBreadcrumbModel.unavailable();
    currentCropSourceIdentity = null;
    currentCropQuery = null;
    currentRawPresentation = new RawJsonPresentation("", new int[] {0});
    syncCropButtonState();
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

  private void syncSearchControls() {
    if (searchSession == null) {
      zoomSearchOccurrenceLabel.setText("Ready");
      zoomSearchPreviousButton.setDisable(true);
      zoomSearchNextButton.setDisable(true);
      syncCropButtonState();
      return;
    }
    zoomSearchOccurrenceLabel.setText(
        searchSession.hasMatches()
            ? (searchSession.activeMatchIndex() + 1) + " of " + searchSession.totalMatches()
            : "0 matches");
    boolean navigationEnabled = searchSession.totalMatches() > 1;
    zoomSearchPreviousButton.setDisable(!navigationEnabled);
    zoomSearchNextButton.setDisable(!navigationEnabled);
    syncCropButtonState();
  }

  private void showSearchError(String errorMessage) {
    zoomSearchErrorLabel.setText(errorMessage == null ? "Invalid regular expression." : errorMessage);
    zoomSearchErrorLabel.setManaged(true);
    zoomSearchErrorLabel.setVisible(true);
  }

  private void hideSearchError() {
    zoomSearchErrorLabel.setText("");
    zoomSearchErrorLabel.setManaged(false);
    zoomSearchErrorLabel.setVisible(false);
  }

  private String searchSourceIdentity() {
    if (currentSnapshot == null) {
      return "zoom";
    }
    return currentSnapshot.windowTitle() + "::" + currentSnapshot.modeLabel();
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
