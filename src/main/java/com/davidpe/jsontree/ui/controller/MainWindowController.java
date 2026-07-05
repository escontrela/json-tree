package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.ClipboardJsonImportResult;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.application.model.JsonSearchExecutionResult;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewViewerPageResult;
import com.davidpe.jsontree.application.model.RawJsonPresentation;
import com.davidpe.jsontree.application.port.in.ImportClipboardJsonUseCase;
import com.davidpe.jsontree.application.port.in.ImportJsonUseCase;
import com.davidpe.jsontree.application.port.out.ClipboardPort;
import com.davidpe.jsontree.application.service.JsonOutlineModelService;
import com.davidpe.jsontree.application.service.JsonSearchWorkflowService;
import com.davidpe.jsontree.application.service.JsonViewerWorkflowService;
import com.davidpe.jsontree.application.service.RawJsonPresentationService;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.ui.model.ViewerVisualState;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.support.AsciiTreeSyntaxHighlighter;
import com.davidpe.jsontree.ui.support.ClipboardImportShortcutSupport;
import com.davidpe.jsontree.ui.support.DroppedJsonPathResolver;
import com.davidpe.jsontree.ui.support.InlineHistoryPreviewState;
import com.davidpe.jsontree.ui.support.InlineHistoryPreviewStateResolver;
import com.davidpe.jsontree.ui.support.LargePreviewDocumentScrollResolver;
import com.davidpe.jsontree.ui.support.LargePreviewIndicatorResolver;
import com.davidpe.jsontree.ui.support.LargePreviewLoadingAffordance;
import com.davidpe.jsontree.ui.support.LargePreviewPageNavigationState;
import com.davidpe.jsontree.ui.support.LargePreviewPageNavigationStateResolver;
import com.davidpe.jsontree.ui.support.LargePreviewViewportState;
import com.davidpe.jsontree.ui.support.LargePreviewViewportStateResolver;
import com.davidpe.jsontree.ui.support.LargePreviewWarningIconFactory;
import com.davidpe.jsontree.ui.support.OutlineMinimapLayout;
import com.davidpe.jsontree.ui.support.OutlineMinimapLayoutPlanner;
import com.davidpe.jsontree.ui.support.OutlineMinimapRow;
import com.davidpe.jsontree.ui.support.OutlineMinimapScrollMapper;
import com.davidpe.jsontree.ui.support.OutlineViewportProjection;
import com.davidpe.jsontree.ui.support.OutlineViewportProjector;
import com.davidpe.jsontree.ui.support.SearchHighlightRange;
import com.davidpe.jsontree.ui.support.SearchMatchProjector;
import com.davidpe.jsontree.ui.support.SearchTextFlowHighlighter;
import com.davidpe.jsontree.ui.support.TextFlowRenderOutcome;
import com.davidpe.jsontree.ui.support.ViewerCapabilityPresentation;
import com.davidpe.jsontree.ui.support.ViewerCapabilityPresentationResolver;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class MainWindowController implements UiScreenController {

  private static final double INLINE_HISTORY_CELL_SIZE = 72.0;
  private static final int INLINE_HISTORY_MIN_VISIBLE_ROWS = 3;
  private static final int INLINE_HISTORY_MAX_VISIBLE_ENTRIES = 10;
  private static final int FILE_NAME_COMPACT_LENGTH_THRESHOLD = 22;
  private static final String FILE_NAME_COMPACT_STYLE = "-fx-font-size: 12px;";
  private static final Duration LARGE_PREVIEW_LOADER_REVEAL_DELAY = Duration.ofMillis(120);
  private static final Duration LARGE_PREVIEW_LOADER_FRAME_INTERVAL = Duration.ofMillis(110);
  private static final double LARGE_PREVIEW_LOGICAL_LINE_HEIGHT = 20.0;

  private static final DateTimeFormatter FILE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss")
          .withLocale(Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  private final AsciiTreeSyntaxHighlighter syntaxHighlighter;
  private final ImportClipboardJsonUseCase importClipboardJsonUseCase;
  private final ImportJsonUseCase importJsonUseCase;
  private final JsonViewerWorkflowService workflowService;
  private final JsonOutlineModelService outlineModelService;
  private final OutlineMinimapLayoutPlanner outlineLayoutPlanner;
  private final OutlineMinimapScrollMapper outlineScrollMapper;
  private final OutlineViewportProjector outlineViewportProjector;
  private final JsonSearchWorkflowService searchWorkflowService;
  private final RawJsonPresentationService rawJsonPresentationService;
  private final ClipboardPort clipboardPort;
  private final UiFlowManager uiFlowManager;
  private final DroppedJsonPathResolver droppedJsonPathResolver;
  private final SearchMatchProjector searchMatchProjector;
  private final SearchTextFlowHighlighter searchTextFlowHighlighter;
  private final ClipboardImportShortcutSupport clipboardImportShortcutSupport;
  private final InlineHistoryPreviewStateResolver inlineHistoryPreviewStateResolver;
  private final LargePreviewIndicatorResolver largePreviewIndicatorResolver;
  private final LargePreviewDocumentScrollResolver largePreviewDocumentScrollResolver;
  private final LargePreviewPageNavigationStateResolver largePreviewPageNavigationStateResolver;
  private final LargePreviewViewportStateResolver largePreviewViewportStateResolver;
  private final ViewerCapabilityPresentationResolver capabilityPresentationResolver;

  public MainWindowController(
      AsciiTreeSyntaxHighlighter syntaxHighlighter,
      ImportClipboardJsonUseCase importClipboardJsonUseCase,
      ImportJsonUseCase importJsonUseCase,
      JsonViewerWorkflowService workflowService,
      JsonOutlineModelService outlineModelService,
      OutlineMinimapLayoutPlanner outlineLayoutPlanner,
      OutlineMinimapScrollMapper outlineScrollMapper,
      OutlineViewportProjector outlineViewportProjector,
      JsonSearchWorkflowService searchWorkflowService,
      RawJsonPresentationService rawJsonPresentationService,
      ClipboardPort clipboardPort,
      DroppedJsonPathResolver droppedJsonPathResolver,
      SearchMatchProjector searchMatchProjector,
      SearchTextFlowHighlighter searchTextFlowHighlighter,
      ClipboardImportShortcutSupport clipboardImportShortcutSupport,
      InlineHistoryPreviewStateResolver inlineHistoryPreviewStateResolver,
      LargePreviewIndicatorResolver largePreviewIndicatorResolver,
      LargePreviewDocumentScrollResolver largePreviewDocumentScrollResolver,
      LargePreviewPageNavigationStateResolver largePreviewPageNavigationStateResolver,
      LargePreviewViewportStateResolver largePreviewViewportStateResolver,
      ViewerCapabilityPresentationResolver capabilityPresentationResolver,
      @Lazy UiFlowManager uiFlowManager) {
    this.syntaxHighlighter = syntaxHighlighter;
    this.importClipboardJsonUseCase = importClipboardJsonUseCase;
    this.importJsonUseCase = importJsonUseCase;
    this.workflowService = workflowService;
    this.outlineModelService = outlineModelService;
    this.outlineLayoutPlanner = outlineLayoutPlanner;
    this.outlineScrollMapper = outlineScrollMapper;
    this.outlineViewportProjector = outlineViewportProjector;
    this.searchWorkflowService = searchWorkflowService;
    this.rawJsonPresentationService = rawJsonPresentationService;
    this.clipboardPort = clipboardPort;
    this.droppedJsonPathResolver = droppedJsonPathResolver;
    this.searchMatchProjector = searchMatchProjector;
    this.searchTextFlowHighlighter = searchTextFlowHighlighter;
    this.clipboardImportShortcutSupport = clipboardImportShortcutSupport;
    this.inlineHistoryPreviewStateResolver = inlineHistoryPreviewStateResolver;
    this.largePreviewIndicatorResolver = largePreviewIndicatorResolver;
    this.largePreviewDocumentScrollResolver = largePreviewDocumentScrollResolver;
    this.largePreviewPageNavigationStateResolver = largePreviewPageNavigationStateResolver;
    this.largePreviewViewportStateResolver = largePreviewViewportStateResolver;
    this.capabilityPresentationResolver = capabilityPresentationResolver;
    this.uiFlowManager = uiFlowManager;
  }

  @FXML private BorderPane rootPane;

  @FXML private Label fileNameLabel;

  @FXML private Label fileMetaLabel;

  @FXML private Label fileLoadedAtValueLabel;

  @FXML private Label fileSourceValueLabel;

  @FXML private Label fileWarningIconLabel;

  @FXML private Label validationStatusLabel;

  @FXML private Label historyInlineMetaLabel;

  @FXML private Label emptyHistoryInlineLabel;

  @FXML private HBox activeSearchStrip;

  @FXML private Label activeSearchQueryLabel;

  @FXML private Label activeSearchOccurrenceLabel;

  @FXML private Label emptyStateLabel;

  @FXML private Label footerStatusLabel;

  @FXML private Label statusStateValueLabel;

  @FXML private Label statusSizeValueLabel;

  @FXML private Label statusLinesValueLabel;

  @FXML private Label statusSourceValueLabel;

  @FXML private Label viewerAidTitleLabel;

  @FXML private Label viewerAidMetaLabel;

  @FXML private ScrollPane viewerScrollPane;

  @FXML private HBox statusRail;

  @FXML private StackPane viewerShell;

  @FXML private VBox fileSummaryCard;

  @FXML private VBox viewerContentBox;

  @FXML private Region largePreviewTopSpacer;

  @FXML private TextFlow treeContentFlow;

  @FXML private TextFlow rawJsonContentFlow;

  @FXML private Region largePreviewBottomSpacer;

  @FXML private StackPane largePreviewLoaderOverlay;

  @FXML private Region largePreviewLoaderSquareOne;

  @FXML private Region largePreviewLoaderSquareTwo;

  @FXML private Region largePreviewLoaderSquareThree;

  @FXML private Region largePreviewLoaderSquareFour;

  @FXML private HBox largePreviewPageControls;

  @FXML private Button largePreviewPreviousButton;

  @FXML private Button largePreviewNextButton;

  @FXML private Label largePreviewCurrentPageLabel;

  @FXML private Label largePreviewTotalPagesLabel;

  @FXML private Button rawJsonButton;

  @FXML private Button searchButton;

  @FXML private Button copyTreeButton;

  @FXML private Button outlineToggleButton;

  @FXML private Button previousSearchButton;

  @FXML private Button nextSearchButton;

  @FXML private VBox searchModalCard;

  @FXML private TextField searchQueryField;

  @FXML private Label searchModalErrorLabel;

  @FXML private ListView<ImportedJsonFile> historyListView;

  @FXML private VBox outlineVBox;

  @FXML private StackPane outlinePreviewShell;

  @FXML private Canvas outlineCanvas;

  @FXML private Region outlineViewportMarker;

  @FXML private Label outlineStateLabel;

  private ViewerVisualState currentState;
  private Instant currentLoadedAt;
  private String currentViewIdentity;
  private boolean windowMetricsLoggingAttached;
  private boolean showingRawJson = false;
  private RawJsonPresentation currentRawJsonPresentation = new RawJsonPresentation("", new int[] {0});
  private JsonOutlineModel currentOutlineModel = JsonOutlineModel.empty();
  private OutlineMinimapLayout currentOutlineLayout = OutlineMinimapLayout.empty();
  private String currentOutlineSourceIdentity;
  private boolean outlineViewportRefreshPending;
  private boolean suppressLargePreviewScrollHandling;
  private boolean largePreviewPageLoadInFlight;
  private double pendingLargePreviewScrollValue = -1.0;
  private long viewerWorkflowLoadSequence;
  private LargePreviewViewportState currentLargePreviewViewportState =
      LargePreviewViewportState.inactive();
  private List<Region> largePreviewLoaderSquares = List.of();
  private LargePreviewLoadingAffordance largePreviewLoadingAffordance;
  private PauseTransition largePreviewLoaderRevealTransition;
  private Timeline largePreviewLoaderAnimationTimeline;
  private long currentLargePreviewLoaderRequestSequence;

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    fileWarningIconLabel.setGraphic(LargePreviewWarningIconFactory.create(16.0));
    configureWindowMetricsLogging();
    rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalKeyPressed);
    rootPane.setOnDragOver(this::handleDragOver);
    rootPane.setOnDragExited(event -> restoreViewFromWorkflow());
    rootPane.setOnDragDropped(this::handleDragDropped);
    historyListView.setFixedCellSize(INLINE_HISTORY_CELL_SIZE);
    double historyListHeight = (INLINE_HISTORY_CELL_SIZE * INLINE_HISTORY_MIN_VISIBLE_ROWS) + 2.0;
    historyListView.setMinHeight(historyListHeight);
    historyListView.setPrefHeight(historyListHeight);
    historyListView.setDisable(true);
    historyListView.addEventFilter(
        MouseEvent.MOUSE_PRESSED,
        event -> {
          if (historyListView.getItems().isEmpty()) {
            event.consume();
          }
        });
    historyListView.setCellFactory(unused -> new InlineHistoryListCell());
    historyListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (unused, oldValue, newValue) -> {
              if (newValue == null || newValue.equals(oldValue)) {
                return;
              }
              Platform.runLater(
                  () -> {
                    reopenHistoryEntry(newValue);
                    historyListView.getSelectionModel().clearSelection();
                  });
            });
    configureLargePreviewLoadingAffordance();
    configureOutlineShell();
    showEmptyViewer();
    refreshInlineHistory();
  }

  private void configureLargePreviewLoadingAffordance() {
    largePreviewLoaderSquares =
        List.of(
            largePreviewLoaderSquareOne,
            largePreviewLoaderSquareTwo,
            largePreviewLoaderSquareThree,
            largePreviewLoaderSquareFour);
    largePreviewLoadingAffordance =
        new LargePreviewLoadingAffordance(
            this::showLargePreviewLoaderOverlay,
            this::hideLargePreviewLoaderOverlay,
            this::applyLargePreviewLoaderFrame);
    hideLargePreviewLoaderOverlay();
  }

  private void configureWindowMetricsLogging() {
    rootPane
        .sceneProperty()
        .addListener(
            (unused, oldScene, newScene) -> {
              if (newScene == null) {
                return;
              }
              newScene
                  .windowProperty()
                  .addListener(
                      (unusedWindow, oldWindow, newWindow) ->
                          attachWindowMetricsLogging(newWindow));
              attachWindowMetricsLogging(newScene.getWindow());
            });
  }

  private void attachWindowMetricsLogging(Window window) {
    if (window == null || windowMetricsLoggingAttached) {
      return;
    }
    windowMetricsLoggingAttached = true;

    ChangeListener<Number> metricsListener =
        (unused, oldValue, newValue) -> printWindowMetrics(window);
    window.xProperty().addListener(metricsListener);
    window.yProperty().addListener(metricsListener);
    window.widthProperty().addListener(metricsListener);
    window.heightProperty().addListener(metricsListener);
    printWindowMetrics(window);
  }

  private void printWindowMetrics(Window window) {
    System.out.printf(
        Locale.ROOT,
        "[window-metrics] x=%.1f y=%.1f width=%.1f height=%.1f%n",
        window.getX(),
        window.getY(),
        window.getWidth(),
        window.getHeight());
  }

  private void configureOutlineShell() {
    ChangeListener<Number> resizeListener = (unused, oldValue, newValue) -> resizeOutlineCanvas();
    outlinePreviewShell.widthProperty().addListener(resizeListener);
    outlinePreviewShell.heightProperty().addListener(resizeListener);
    outlinePreviewShell.setOnMousePressed(this::handleOutlineInteraction);
    outlinePreviewShell.setOnMouseDragged(this::handleOutlineInteraction);
    viewerScrollPane
        .vvalueProperty()
        .addListener(
            (unused, oldValue, newValue) -> {
              scheduleOutlineViewportRefresh();
              handleViewerScrollValueChanged(newValue.doubleValue());
            });
    viewerScrollPane
        .viewportBoundsProperty()
        .addListener((unused, oldValue, newValue) -> scheduleOutlineViewportRefresh());
    viewerContentBox
        .layoutBoundsProperty()
        .addListener((unused, oldValue, newValue) -> scheduleOutlineViewportRefresh());
    resizeOutlineCanvas();
  }

  private void resizeOutlineCanvas() {
    double width = Math.max(0.0, outlinePreviewShell.getWidth() - 2.0);
    double height = Math.max(0.0, outlinePreviewShell.getHeight() - 2.0);
    outlineCanvas.setWidth(width);
    outlineCanvas.setHeight(height);
    refreshOutlineCanvas();
  }

  private void showOutlineEmptyShell() {
    currentOutlineLayout = OutlineMinimapLayout.empty();
    showOutlineShellState(
        "Awaiting JSON",
        "Load a valid JSON file to populate the outline minimap shell.",
        "The right rail keeps a dedicated preview surface reserved for the minimap.",
        null);
  }

  private void showOutlineValidShell(AsciiTreeDocument document) {
    viewerAidTitleLabel.setText("JSON outline");
    viewerAidMetaLabel.setText(
        currentOutlineModel.totalEntries()
            + " outline nodes • depth "
            + currentOutlineModel.maxDepth()
            + " • "
            + document.lineCount()
            + " viewer lines");
    outlineCanvas.setManaged(true);
    outlineCanvas.setVisible(true);
    outlineStateLabel.setManaged(false);
    outlineStateLabel.setVisible(false);
    outlineViewportMarker.setManaged(false);
    outlineViewportMarker.setVisible(false);
    outlinePreviewShell
        .getStyleClass()
        .removeAll("outline-state-loading", "outline-state-valid", "outline-state-invalid");
    outlinePreviewShell.getStyleClass().add("outline-state-valid");
    drawOutlineMinimap();
    scheduleOutlineViewportRefresh();
  }

  private void showOutlineShellState(
      String title, String stateMessage, String metaMessage, String previewStateClass) {
    viewerAidTitleLabel.setText(title);
    viewerAidMetaLabel.setText(metaMessage);
    outlineCanvas.setManaged(true);
    outlineCanvas.setVisible(true);
    outlineStateLabel.setText(stateMessage);
    outlineStateLabel.setManaged(true);
    outlineStateLabel.setVisible(true);
    outlineViewportMarker.setManaged(false);
    outlineViewportMarker.setVisible(false);
    outlinePreviewShell
        .getStyleClass()
        .removeAll("outline-state-loading", "outline-state-valid", "outline-state-invalid");
    if (previewStateClass != null) {
      outlinePreviewShell.getStyleClass().add(previewStateClass);
    }
    currentOutlineLayout = OutlineMinimapLayout.empty();
    drawOutlineShellPlaceholder();
  }

  private void refreshOutlineCanvas() {
    if (outlineStateLabel.isVisible() || currentOutlineModel.emptyModel() || !outlineCanvas.isVisible()) {
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
            currentOutlineModel, outlineCanvas.getWidth(), outlineCanvas.getHeight());

    GraphicsContext graphics = outlineCanvas.getGraphicsContext2D();
    double width = outlineCanvas.getWidth();
    double height = outlineCanvas.getHeight();
    graphics.clearRect(0, 0, width, height);
    if (currentOutlineLayout.emptyLayout()) {
      return;
    }

    graphics.setFill(Color.web("#f6f8fb"));
    graphics.fillRect(0, 0, width, height);

    for (OutlineMinimapRow row : currentOutlineLayout.rows()) {
      graphics.setFill(outlineRowColor(row));
      graphics.fillRoundRect(row.x(), row.y(), row.width(), row.height(), 2.0, 2.0);
    }
  }

  private Color outlineRowColor(OutlineMinimapRow row) {
    return switch (row.kind()) {
      case OBJECT -> Color.web("#3569a3");
      case ARRAY -> Color.web("#6f8bad");
      case VALUE -> Color.web("#b5c0cd");
    };
  }

  private void handleOutlineInteraction(MouseEvent event) {
    if (currentState != ViewerVisualState.VALID || currentOutlineLayout.emptyLayout()) {
      return;
    }

    double contentHeight = viewerContentBox.getLayoutBounds().getHeight();
    double viewportHeight = viewerScrollPane.getViewportBounds().getHeight();
    double scrollValue =
        outlineScrollMapper.scrollValueForPointer(
            event.getY(), outlinePreviewShell.getHeight(), viewportHeight, contentHeight);
    viewerScrollPane.setVvalue(scrollValue);
    event.consume();
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

  private void refreshOutlineViewportMarker() {
    if (currentOutlineLayout.emptyLayout() || !outlinePreviewShell.isVisible()) {
      hideOutlineViewportMarker();
      return;
    }

    OutlineViewportProjection projection =
        outlineViewportProjector.project(
            viewerScrollPane.getVvalue(),
            outlineCanvas.getHeight(),
            viewerScrollPane.getViewportBounds().getHeight(),
            viewerContentBox.getLayoutBounds().getHeight());
    if (!projection.visible()) {
      hideOutlineViewportMarker();
      return;
    }

    double markerWidth = Math.max(24.0, outlineCanvas.getWidth() - 20.0);
    outlineViewportMarker.resizeRelocate(
        10.0, 1.0 + projection.y(), markerWidth, projection.height());
    outlineViewportMarker.setManaged(false);
    outlineViewportMarker.setVisible(true);
  }

  private void hideOutlineViewportMarker() {
    outlineViewportMarker.setManaged(false);
    outlineViewportMarker.setVisible(false);
  }

  private void drawOutlineShellPlaceholder() {
    GraphicsContext graphics = outlineCanvas.getGraphicsContext2D();
    double width = outlineCanvas.getWidth();
    double height = outlineCanvas.getHeight();
    graphics.clearRect(0, 0, width, height);
    if (width <= 0.0 || height <= 0.0) {
      return;
    }

    graphics.setFill(Color.web("#eef1f4"));
    graphics.fillRect(0, 0, width, height);

    double rowCount = 8.0;
    double rowHeight = Math.max(6.0, (height - 36.0) / rowCount);
    for (int index = 0; index < rowCount; index++) {
      double x = 14.0 + ((index % 3) * 10.0);
      double y = 18.0 + (index * rowHeight);
      double barWidth = Math.max(20.0, width - x - (18.0 + ((index % 4) * 6.0)));
      graphics.setFill(index % 2 == 0 ? Color.web("#d5dbe3") : Color.web("#c8d1dc"));
      graphics.fillRect(x, y, barWidth, 3.0);
    }
  }

  @Override
  public void onShow() {
    refreshInlineHistory();
    syncActiveSearchStrip();
    restoreViewFromWorkflow();
  }

  public void renderAsciiTree(JsonViewerLoadResult result) {
    renderAsciiTree(result, 0.0);
  }

  private void renderAsciiTree(JsonViewerLoadResult result, double targetVerticalScrollValue) {
    AsciiTreeDocument document = result.asciiTreeDocument();
    syncLargePreviewViewportState(result, targetVerticalScrollValue);
    applyCapabilityPresentation(result);
    syncLargePreviewPageControls(result);
    resetViewModeIfNeeded();
    applyLargePreviewDocumentScrollShell(result);
    TextFlowRenderOutcome renderOutcome =
        syntaxHighlighter.appendHighlightedContent(
        treeContentFlow, document, currentAsciiHighlightRanges(document));
    treeContentFlow.setManaged(true);
    treeContentFlow.setVisible(true);
    rawJsonContentFlow.setManaged(false);
    rawJsonContentFlow.setVisible(false);
    emptyStateLabel.setManaged(false);
    emptyStateLabel.setVisible(false);
    updateOutlineShell(result, document);
    setViewerScrollPosition(0.0, targetVerticalScrollValue);
    applyState(ViewerVisualState.VALID);
    scheduleOutlineViewportRefresh();
    if (renderOutcome.guardrailApplied()) {
      footerStatusLabel.setText("Render budget guard active • showing simplified tree");
    }
  }

  public void showEmptyViewer() {
    cancelLargePreviewLoadingAffordance();
    resetOutlineModel();
    clearLargePreviewViewportState();
    hideLargePreviewPageControls();
    hideLargePreviewDocumentScrollShell();
    currentLoadedAt = null;
    currentViewIdentity = null;
    resetToolbarForNonRenderableState();
    showFileWarningIcon(false);
    updateFileNameLabel("No file loaded");
    fileMetaLabel.setText("Drop a JSON anywhere in the window");
    fileLoadedAtValueLabel.setText("Not loaded");
    fileSourceValueLabel.setText("Waiting for import");
    showOutlineEmptyShell();
    treeContentFlow.getChildren().clear();
    treeContentFlow.setManaged(false);
    treeContentFlow.setVisible(false);
    emptyStateLabel.setManaged(true);
    emptyStateLabel.setVisible(true);
    emptyStateLabel.setText("Drop a JSON anywhere in the window");
    setValidationBadge("Waiting", "status-idle");
    footerStatusLabel.setText("No JSON loaded");
    setStatusRailValues("EMPTY", "--", "--", "Waiting for import");
    viewerContentBox.autosize();
    searchWorkflowService.clear();
    syncActiveSearchStrip();
    hideSearchModal();
    resetViewModeIfNeeded();
    applyState(ViewerVisualState.EMPTY);
  }

  public void showDraggingState() {
    cancelLargePreviewLoadingAffordance();
    showFileWarningIcon(false);
    emptyStateLabel.setText("Release to inspect this JSON file");
    showOutlineShellState(
        "Drop ready",
        "Release to prepare the outline minimap shell for this JSON file.",
        "The outline panel remains docked and ready for the incoming document.",
        "outline-state-loading");
    setValidationBadge("Drop ready", "status-accent");
    footerStatusLabel.setText("Waiting for JSON drop");
    setStatusRailValues("DROP READY", "--", "--", "Drag payload");
    applyState(ViewerVisualState.DRAGGING);
  }

  public void showLoadingState(String fileName) {
    cancelLargePreviewLoadingAffordance();
    resetOutlineModel();
    clearLargePreviewViewportState();
    hideLargePreviewPageControls();
    hideLargePreviewDocumentScrollShell();
    currentLoadedAt = Instant.now();
    currentViewIdentity = "loading:" + fileName;
    resetToolbarForNonRenderableState();
    showFileWarningIcon(false);
    updateFileNameLabel(fileName);
    fileMetaLabel.setText("Preparing JSON preview");
    fileLoadedAtValueLabel.setText(FILE_TIME_FORMATTER.format(currentLoadedAt));
    fileSourceValueLabel.setText("Local file");
    showOutlineShellState(
        "Preparing outline",
        "Building the outline minimap shell for this JSON preview.",
        "The panel keeps a reserved minimap surface while validation completes.",
        "outline-state-loading");
    setValidationBadge("Loading", "status-muted");
    footerStatusLabel.setText("Parsing JSON");
    setStatusRailValues("LOADING", "--", "--", "Local file");
    emptyStateLabel.setText("Loading JSON preview...");
    treeContentFlow.getChildren().clear();
    treeContentFlow.setManaged(false);
    treeContentFlow.setVisible(false);
    emptyStateLabel.setManaged(true);
    emptyStateLabel.setVisible(true);
    searchWorkflowService.clear();
    syncActiveSearchStrip();
    hideSearchModal();
    resetViewModeIfNeeded();
    applyState(ViewerVisualState.LOADING);
  }

  public void showInvalidState(String message) {
    cancelLargePreviewLoadingAffordance();
    resetOutlineModel();
    clearLargePreviewViewportState();
    hideLargePreviewPageControls();
    hideLargePreviewDocumentScrollShell();
    resetToolbarForNonRenderableState();
    showOutlineShellState(
        "Outline unavailable",
        "The current JSON payload cannot produce an outline minimap.",
        "Fix the document or reopen a valid snapshot to restore the minimap.",
        "outline-state-invalid");
    setValidationBadge("Invalid", "status-error");
    footerStatusLabel.setText("JSON needs attention");
    if ("VALID".equals(statusStateValueLabel.getText())) {
      statusStateValueLabel.setText("INVALID");
    }
    emptyStateLabel.setText(message);
    treeContentFlow.getChildren().clear();
    treeContentFlow.setManaged(false);
    treeContentFlow.setVisible(false);
    emptyStateLabel.setManaged(true);
    emptyStateLabel.setVisible(true);
    searchWorkflowService.clear();
    syncActiveSearchStrip();
    hideSearchModal();
    resetViewModeIfNeeded();
    applyState(ViewerVisualState.INVALID);
  }

  public void showEmptyFileState() {
    showInvalidState("The JSON file is empty.");
    viewerAidTitleLabel.setText("Empty file");
    viewerAidMetaLabel.setText(
        "The selected file exists but does not contain any JSON content to render.");
    outlineStateLabel.setText("The outline minimap cannot render because the file is empty.");
    footerStatusLabel.setText("The JSON file is empty");
  }

  ViewerVisualState currentState() {
    return currentState;
  }

  private void applyState(ViewerVisualState state) {
    currentState = state;
    viewerShell
        .getStyleClass()
        .removeAll("viewer-dragging", "viewer-loading", "viewer-valid", "viewer-invalid");
    statusRail
        .getStyleClass()
        .removeAll("shell-dragging", "shell-loading", "shell-valid", "shell-invalid");
    fileSummaryCard
        .getStyleClass()
        .removeAll("shell-dragging", "shell-loading", "shell-valid", "shell-invalid");
    switch (state) {
      case DRAGGING -> {
        viewerShell.getStyleClass().add("viewer-dragging");
        statusRail.getStyleClass().add("shell-dragging");
        fileSummaryCard.getStyleClass().add("shell-dragging");
      }
      case LOADING -> {
        viewerShell.getStyleClass().add("viewer-loading");
        statusRail.getStyleClass().add("shell-loading");
        fileSummaryCard.getStyleClass().add("shell-loading");
      }
      case VALID -> {
        viewerShell.getStyleClass().add("viewer-valid");
        statusRail.getStyleClass().add("shell-valid");
        fileSummaryCard.getStyleClass().add("shell-valid");
      }
      case INVALID -> {
        viewerShell.getStyleClass().add("viewer-invalid");
        statusRail.getStyleClass().add("shell-invalid");
        fileSummaryCard.getStyleClass().add("shell-invalid");
      }
      case EMPTY -> {
        // Base style only.
      }
    }
  }

  private void handleDragOver(DragEvent event) {
    if (firstJsonFile(event.getDragboard()).isEmpty()) {
      return;
    }
    event.acceptTransferModes(TransferMode.COPY);
    showDraggingState();
    event.consume();
  }

  private void handleDragDropped(DragEvent event) {
    Path jsonPath = firstJsonFile(event.getDragboard()).orElse(null);
    if (jsonPath == null) {
      event.setDropCompleted(false);
      restoreViewFromWorkflow();
      return;
    }

    loadImportedFileAsync(importJsonUseCase.importFile(jsonPath));
    event.setDropCompleted(true);
    event.consume();
  }

  private java.util.Optional<Path> firstJsonFile(Dragboard dragboard) {
    if (!dragboard.hasFiles()) {
      return java.util.Optional.empty();
    }
    return droppedJsonPathResolver.resolve(dragboard.getFiles());
  }

  private void handleGlobalKeyPressed(KeyEvent event) {
    if (!clipboardImportShortcutSupport.shouldTrigger(
        event.getCode(),
        event.isShortcutDown(),
        event.isAltDown(),
        event.isShiftDown(),
        event.getTarget() instanceof TextInputControl,
        searchModalCard.isVisible())) {
      return;
    }

    ClipboardJsonImportResult result = importClipboardJsonUseCase.importFromClipboard();
    if (result.successful()) {
      presentLoadResult(result.loadResult());
      event.consume();
      return;
    }
    presentClipboardImportFailure(result);
    event.consume();
  }

  private void restoreViewFromWorkflow() {
    workflowService.currentView().ifPresentOrElse(this::presentLoadResult, this::showEmptyViewer);
  }

  private void loadImportedFileAsync(com.davidpe.jsontree.domain.model.JsonImportResult importResult) {
    showLoadingState(importResult.fileName());
    beginLargePreviewLoadingAffordance();
    long requestSequence = ++viewerWorkflowLoadSequence;
    CompletableFuture
        .supplyAsync(() -> workflowService.loadImportedFile(importResult))
        .whenComplete(
            (result, throwable) ->
                Platform.runLater(
                    () ->
                        handleImportedFileLoadResult(
                            requestSequence, importResult.fileName(), result, throwable)));
  }

  private void handleImportedFileLoadResult(
      long requestSequence,
      String fileName,
      JsonViewerLoadResult result,
      Throwable throwable) {
    if (requestSequence != viewerWorkflowLoadSequence) {
      return;
    }
    cancelLargePreviewLoadingAffordance();
    if (throwable != null || result == null) {
      showInvalidState("Unable to load JSON file: " + fileName);
      footerStatusLabel.setText("JSON load failed");
      return;
    }
    presentLoadResult(result);
  }

  private void handleHistoryReopenResult(
      long requestSequence,
      String fileName,
      java.util.Optional<JsonViewerLoadResult> result,
      Throwable throwable) {
    if (requestSequence != viewerWorkflowLoadSequence) {
      return;
    }
    cancelLargePreviewLoadingAffordance();
    if (throwable != null) {
      showInvalidState("Unable to reopen history snapshot: " + fileName);
      footerStatusLabel.setText("History reopen failed");
      return;
    }
    result.ifPresentOrElse(
        this::presentLoadResult,
        () -> {
          showInvalidState("Stored history snapshot is no longer available.");
          footerStatusLabel.setText("History snapshot unavailable");
        });
  }

  private void presentLoadResult(JsonViewerLoadResult result) {
    updateFileSummary(result);
    searchWorkflowService.clearIfSourceChanged(currentViewIdentity(result));
    if (!result.capabilities().searchAvailable()) {
      searchWorkflowService.clear();
      hideSearchModal();
    }
    syncActiveSearchStrip();
    syncStatusRail(result);
    refreshInlineHistory();

    JsonValidationResult validationResult = result.validationResult();
    if (validationResult.status() == JsonValidationStatus.VALID && result.hasRenderableTree()) {
      renderAsciiTree(result);
      return;
    }
    if (validationResult.status() == JsonValidationStatus.EMPTY) {
      showEmptyFileState();
      return;
    }
    showInvalidState(composeValidationMessage(validationResult));
  }

  private void presentClipboardImportFailure(ClipboardJsonImportResult result) {
    if (workflowService.currentView().isPresent()) {
      restoreViewFromWorkflow();
      footerStatusLabel.setText(result.message());
      return;
    }

    resetOutlineModel();
    currentLoadedAt = Instant.now();
    currentViewIdentity = "clipboard-error:" + currentLoadedAt.toEpochMilli();
    showFileWarningIcon(false);
    updateFileNameLabel("Clipboard JSON");
    fileMetaLabel.setText(
        "Paste valid JSON using Command+P or Command+V on macOS, Ctrl+P or Ctrl+V elsewhere.");
    fileLoadedAtValueLabel.setText(FILE_TIME_FORMATTER.format(currentLoadedAt));
    fileSourceValueLabel.setText("Clipboard");
    showInvalidState(result.message());
    viewerAidTitleLabel.setText("Clipboard import failed");
    viewerAidMetaLabel.setText(
        "The clipboard stays external until it contains valid JSON that can become a temporary"
            + " local document.");
    outlineStateLabel.setText("Clipboard content did not produce a valid JSON outline.");
    footerStatusLabel.setText(result.message());
    setStatusRailValues("INVALID", "--", "--", "Clipboard");
  }

  private String formatFileMeta(long sizeBytes, JsonDocumentSourceKind sourceKind) {
    String meta = formatBytes(sizeBytes);
    return switch (sourceKind) {
      case HISTORY -> meta + " • reopened from history";
      case CLIPBOARD -> meta + " • clipboard import";
      case LOCAL_FILE -> meta + " • local import";
    };
  }

  private String formatBytes(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    CharacterIterator iterator = new StringCharacterIterator("KMGTPE");
    double scaled = bytes;
    while (scaled >= 1024 && iterator.current() != 'E') {
      scaled /= 1024;
      iterator.next();
    }
    return String.format(java.util.Locale.ROOT, "%.1f %cB", scaled, iterator.current());
  }

  private String composeValidationMessage(JsonValidationResult validationResult) {
    if (validationResult.line() == null || validationResult.column() == null) {
      return validationResult.message();
    }
    return validationResult.message()
        + " (line "
        + validationResult.line()
        + ", column "
        + validationResult.column()
        + ")";
  }

  private void updateFileNameLabel(String fileName) {
    fileNameLabel.setText(fileName);
    if (fileName != null && fileName.length() > FILE_NAME_COMPACT_LENGTH_THRESHOLD) {
      fileNameLabel.setStyle(FILE_NAME_COMPACT_STYLE);
      return;
    }
    fileNameLabel.setStyle("");
  }

  private void updateFileSummary(JsonViewerLoadResult result) {
    ViewerCapabilityPresentation presentation = capabilityPresentationResolver.resolve(result);
    updateFileNameLabel(result.importResult().fileName());
    showFileWarningIcon(largePreviewIndicatorResolver.showForCurrentView(result));
    fileMetaLabel.setText(
        formatFileMeta(result.importResult().sizeBytes(), result.importResult().sourceKind())
            + presentation.fileMetaSuffix());
    fileLoadedAtValueLabel.setText(FILE_TIME_FORMATTER.format(resolveLoadedAt(result)));
    fileSourceValueLabel.setText(sourceLabel(result.importResult().sourceKind()));
  }

  private Instant resolveLoadedAt(JsonViewerLoadResult result) {
    String identity = currentViewIdentity(result);
    if (!identity.equals(currentViewIdentity)) {
      currentLoadedAt =
          result.importResult().sourceKind() == JsonDocumentSourceKind.HISTORY
                  && result.historyEntry() != null
              ? result.historyEntry().importedAt()
              : Instant.now();
      currentViewIdentity = identity;
    }
    return currentLoadedAt;
  }

  private String currentViewIdentity(JsonViewerLoadResult result) {
    if (result.importResult().sourceKind() == JsonDocumentSourceKind.HISTORY
        && result.historyEntry() != null) {
      return "history:" + result.historyEntry().storedName();
    }
    String prefix =
        switch (result.importResult().sourceKind()) {
          case CLIPBOARD -> "clipboard:";
          case LOCAL_FILE -> "file:";
          case HISTORY -> "history:";
        };
    return prefix + result.importResult().path().toAbsolutePath().normalize();
  }

  private void syncStatusRail(JsonViewerLoadResult result) {
    ViewerCapabilityPresentation presentation = capabilityPresentationResolver.resolve(result);
    String state =
        result.validationResult().status() == JsonValidationStatus.VALID
            ? presentation.statusState()
            : switch (result.validationResult().status()) {
              case EMPTY -> "EMPTY";
              case INVALID, PARSING_ERROR -> "INVALID";
              case VALID -> presentation.statusState();
            };
    String lines =
        result.hasRenderableTree()
            ? Integer.toString(result.asciiTreeDocument().lineCount())
            : "--";
    setStatusRailValues(
        state,
        formatBytes(result.importResult().sizeBytes()),
        lines,
        sourceLabel(result.importResult().sourceKind()));
  }

  private String sourceLabel(JsonDocumentSourceKind sourceKind) {
    return switch (sourceKind) {
      case HISTORY -> "History snapshot";
      case CLIPBOARD -> "Clipboard";
      case LOCAL_FILE -> "Local file";
    };
  }

  private void setStatusRailValues(String state, String size, String lines, String source) {
    statusStateValueLabel.setText(state);
    statusSizeValueLabel.setText(size);
    statusLinesValueLabel.setText(lines);
    statusSourceValueLabel.setText(source);
  }

  private void refreshInlineHistory() {
    List<ImportedJsonFile> entries = workflowService.loadHistoryEntries();
    InlineHistoryPreviewState previewState =
        inlineHistoryPreviewStateResolver.resolve(entries, INLINE_HISTORY_MAX_VISIBLE_ENTRIES);
    historyInlineMetaLabel.setText(previewState.summaryLabel());

    if (previewState.visibleEntries().isEmpty()) {
      historyListView.getItems().clear();
      historyListView.setDisable(true);
      historyListView.setManaged(false);
      historyListView.setVisible(false);
      emptyHistoryInlineLabel.setManaged(true);
      emptyHistoryInlineLabel.setVisible(true);
      return;
    }

    emptyHistoryInlineLabel.setManaged(false);
    emptyHistoryInlineLabel.setVisible(false);
    historyListView.setDisable(false);
    historyListView.setManaged(true);
    historyListView.setVisible(true);
    historyListView.getItems().setAll(previewState.visibleEntries());
  }

  private void reopenHistoryEntry(ImportedJsonFile entry) {
    showLoadingState(entry.originalName());
    fileMetaLabel.setText("Preparing JSON preview from history");
    fileSourceValueLabel.setText("History");
    setStatusRailValues("LOADING", "--", "--", "History");
    beginLargePreviewLoadingAffordance();
    long requestSequence = ++viewerWorkflowLoadSequence;
    CompletableFuture
        .supplyAsync(() -> workflowService.reopenHistoryEntry(entry.storedName()))
        .whenComplete(
            (result, throwable) ->
                Platform.runLater(
                    () ->
                        handleHistoryReopenResult(
                            requestSequence, entry.originalName(), result, throwable)));
  }

  private final class InlineHistoryListCell extends ListCell<ImportedJsonFile> {

    private final Label titleLabel = new Label();
    private final Label metaLabel = new Label();
    private final HBox titleRow = new HBox(8.0);
    private final javafx.scene.image.ImageView warningIcon =
        LargePreviewWarningIconFactory.create(12.0);
    private final VBox content = new VBox(4.0);

    private InlineHistoryListCell() {
      titleLabel.getStyleClass().add("history-inline-title");
      metaLabel.getStyleClass().add("history-inline-meta");
      warningIcon.setManaged(false);
      warningIcon.setVisible(false);
      titleRow.getChildren().addAll(titleLabel, warningIcon);
      content.getChildren().addAll(titleRow, metaLabel);
    }

    @Override
    protected void updateItem(ImportedJsonFile item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
        return;
      }
      titleLabel.setText(item.originalName());
      boolean showWarning = largePreviewIndicatorResolver.showForHistoryEntry(item);
      warningIcon.setManaged(showWarning);
      warningIcon.setVisible(showWarning);
      metaLabel.setText(
          FILE_TIME_FORMATTER.format(item.importedAt())
              + " • "
              + formatBytes(item.sizeBytes())
              + " • "
              + item.lineCount()
              + " lines");
      setText(null);
      setGraphic(content);
    }
  }

  @FXML
  void toggleRawJson() {
    if (rawJsonButton.isDisable()) {
      return;
    }
    if (showingRawJson) {
      workflowService
          .currentView()
          .filter(result -> result.validationResult().status() == JsonValidationStatus.VALID)
          .filter(JsonViewerLoadResult::hasRenderableTree)
          .ifPresent(this::renderAsciiTree);
    } else {
      workflowService.currentViewRawJson().ifPresent(this::renderRawJsonContent);
    }
    viewerScrollPane.setHvalue(0);
    viewerScrollPane.setVvalue(0);
    scheduleOutlineViewportRefresh();
  }

  private void resetViewModeIfNeeded() {
    if (!showingRawJson) {
      return;
    }
    rawJsonContentFlow.getChildren().clear();
    rawJsonContentFlow.setManaged(false);
    rawJsonContentFlow.setVisible(false);
    rawJsonButton.setText("Raw JSON");
    showingRawJson = false;
    currentRawJsonPresentation = new RawJsonPresentation("", new int[] {0});
  }

  private void setValidationBadge(String text, String styleClass) {
    validationStatusLabel.setText(text);
    validationStatusLabel
        .getStyleClass()
        .removeAll("status-idle", "status-accent", "status-muted", "status-valid", "status-error");
    validationStatusLabel.getStyleClass().add(styleClass);
  }

  @FXML
  void openHistory() {
    uiFlowManager.show(UiScreenId.HISTORY);
  }

  @FXML
  void showPreviousLargePreviewPage() {
    navigateLargePreviewByStep(-1);
  }

  @FXML
  void showNextLargePreviewPage() {
    navigateLargePreviewByStep(1);
  }

  @FXML
  void openSearchModal() {
    if (searchButton.isDisable()) {
      return;
    }
    searchQueryField.setText(
        searchWorkflowService.currentSession().map(JsonSearchSession::query).orElse(""));
    searchModalErrorLabel.setManaged(false);
    searchModalErrorLabel.setVisible(false);
    searchModalErrorLabel.setText("");
    searchModalCard.setManaged(true);
    searchModalCard.setVisible(true);
    Platform.runLater(
        () -> {
          searchQueryField.requestFocus();
          searchQueryField.selectAll();
        });
  }

  @FXML
  void cancelSearchModal() {
    hideSearchModal();
  }

  @FXML
  void acceptSearchModal() {
    JsonSearchExecutionResult result =
        searchWorkflowService.activateSearch(
            currentViewIdentity == null ? "current-view" : currentViewIdentity,
            searchQueryField.getText());
    if (!result.successful()) {
      searchModalErrorLabel.setText(result.errorMessage());
      searchModalErrorLabel.setManaged(true);
      searchModalErrorLabel.setVisible(true);
      return;
    }

    syncActiveSearchStrip();
    hideSearchModal();
    refreshCurrentViewerContent();
    scrollToActiveSearchHighlight();
  }

  @FXML
  void clearSearchSession() {
    searchWorkflowService.clear();
    syncActiveSearchStrip();
    refreshCurrentViewerContent();
  }

  @FXML
  void showPreviousSearchResult() {
    searchWorkflowService
        .moveToPreviousMatch()
        .ifPresent(
            unused -> {
              syncActiveSearchStrip();
              refreshCurrentViewerContent();
              scrollToActiveSearchHighlight();
            });
  }

  @FXML
  void showNextSearchResult() {
    searchWorkflowService
        .moveToNextMatch()
        .ifPresent(
            unused -> {
              syncActiveSearchStrip();
              refreshCurrentViewerContent();
              scrollToActiveSearchHighlight();
            });
  }

  @FXML
  void copyTree() {
    workflowService
        .currentView()
        .map(JsonViewerLoadResult::asciiTreeDocument)
        .map(AsciiTreeDocument::content)
        .ifPresent(clipboardPort::copy);
  }

  @FXML
  void toggleOutline() {
    if (outlineToggleButton.isDisable()) {
      return;
    }
    boolean nextVisible = !outlineVBox.isVisible();
    outlineVBox.setVisible(nextVisible);
    outlineVBox.setManaged(nextVisible);
    if (nextVisible) {
      resizeOutlineCanvas();
      scheduleOutlineViewportRefresh();
      return;
    }
    hideOutlineViewportMarker();
  }

  private void hideSearchModal() {
    searchModalCard.setManaged(false);
    searchModalCard.setVisible(false);
  }

  private void syncOutlineModelWithCurrentView() {
    if (currentViewIdentity == null) {
      resetOutlineModel();
      return;
    }
    workflowService
        .currentView()
        .filter(result -> result.capabilities().outlineAvailable())
        .ifPresentOrElse(
            result -> {
              String outlineIdentity = outlineIdentity(result);
              if (outlineIdentity.equals(currentOutlineSourceIdentity)) {
                return;
              }
              currentOutlineModel = outlineModelForCurrentView(result);
              currentOutlineSourceIdentity = outlineIdentity;
            },
            this::resetOutlineModel);
  }

  private JsonOutlineModel outlineModelForCurrentView(JsonViewerLoadResult result) {
    if (result.usesLargePreview()) {
      return workflowService
          .currentLargePreviewOutlineDigest()
          .map(outlineModelService::buildFromLargePreviewDigest)
          .orElseGet(() -> outlineModelService.buildFromAsciiPreview(result.asciiTreeDocument()));
    }
    return workflowService
        .currentViewRawJson()
        .map(outlineModelService::buildFromRawJson)
        .orElse(JsonOutlineModel.empty());
  }

  private String outlineIdentity(JsonViewerLoadResult result) {
    if (result.usesLargePreview() && result.hasLargePreviewSession()) {
      return result.largePreviewSession().sessionId()
          + ":digest:"
          + result.largePreviewSession().outlineDigestReady();
    }
    return currentViewIdentity;
  }

  private void resetOutlineModel() {
    currentOutlineModel = JsonOutlineModel.empty();
    currentOutlineLayout = OutlineMinimapLayout.empty();
    currentOutlineSourceIdentity = null;
  }

  private void refreshCurrentViewerContent() {
    workflowService
        .currentView()
        .filter(result -> result.validationResult().status() == JsonValidationStatus.VALID)
        .ifPresent(
            result -> {
              if (showingRawJson) {
                if (result.capabilities().rawJsonAvailable()) {
                  workflowService.currentViewRawJson().ifPresent(this::renderRawJsonContent);
                  return;
                }
                resetViewModeIfNeeded();
              }
              if (result.hasRenderableTree()) {
                renderAsciiTree(result);
                return;
              }
            });
  }

  private void handleViewerScrollValueChanged(double verticalScrollValue) {
    if (suppressLargePreviewScrollHandling || showingRawJson) {
      return;
    }
    if (largePreviewPageLoadInFlight) {
      pendingLargePreviewScrollValue = clamp(verticalScrollValue);
      return;
    }
    workflowService
        .currentView()
        .ifPresent(
            result -> {
              largePreviewViewportStateResolver
                  .resolveForScroll(result, verticalScrollValue)
                  .ifPresent(
                      targetViewportState -> {
                        currentLargePreviewViewportState = targetViewportState;
                        if (targetViewportState.currentPageIndex()
                            != result.largePreviewSession().currentPageIndex()) {
                          requestLargePreviewPage(targetViewportState);
                        }
                      });
            });
  }

  private void requestLargePreviewPage(LargePreviewViewportState targetViewportState) {
    if (largePreviewPageLoadInFlight) {
      return;
    }
    workflowService
        .currentView()
        .filter(JsonViewerLoadResult::hasLargePreviewSession)
        .ifPresent(
            result -> {
              largePreviewPageLoadInFlight = true;
              currentLargePreviewViewportState = targetViewportState;
              pendingLargePreviewScrollValue = -1.0;
              beginLargePreviewLoadingAffordance();
              String sessionId = result.largePreviewSession().sessionId();
              CompletableFuture
                  .supplyAsync(
                      () ->
                          workflowService.loadLargePreviewPage(
                              sessionId, targetViewportState.currentPageIndex()))
                  .whenComplete(
                      (pageResult, throwable) ->
                          Platform.runLater(
                              () ->
                                  handleLargePreviewPageResult(
                                      sessionId,
                                      pageResult,
                                      throwable,
                                      targetViewportState)));
            });
  }

  private void handleLargePreviewPageResult(
      String expectedSessionId,
      java.util.Optional<LargePreviewViewerPageResult> pageResult,
      Throwable throwable,
      LargePreviewViewportState targetViewportState) {
    cancelLargePreviewLoadingAffordance();
    largePreviewPageLoadInFlight = false;
    if (throwable != null || pageResult == null || pageResult.isEmpty()) {
      return;
    }

    workflowService
        .currentView()
        .filter(JsonViewerLoadResult::hasLargePreviewSession)
        .filter(result -> result.largePreviewSession().sessionId().equals(expectedSessionId))
        .ifPresent(unused -> presentLargePreviewPage(pageResult.get(), targetViewportState));
  }

  private void presentLargePreviewPage(
      LargePreviewViewerPageResult pageResult, LargePreviewViewportState targetViewportState) {
    updateFileSummary(pageResult.loadResult());
    syncStatusRail(pageResult.loadResult());
    renderAsciiTree(pageResult.loadResult(), targetViewportState.globalScrollValue());
    processDeferredLargePreviewScroll();
  }

  private void showLargePreviewLoaderOverlay() {
    largePreviewLoaderOverlay.setManaged(true);
    largePreviewLoaderOverlay.setVisible(true);
    if (!viewerShell.getStyleClass().contains("viewer-large-preview-waiting")) {
      viewerShell.getStyleClass().add("viewer-large-preview-waiting");
    }
  }

  private void hideLargePreviewLoaderOverlay() {
    largePreviewLoaderOverlay.setManaged(false);
    largePreviewLoaderOverlay.setVisible(false);
    viewerShell.getStyleClass().remove("viewer-large-preview-waiting");
    applyLargePreviewLoaderFrame(-1);
  }

  private void applyLargePreviewLoaderFrame(int activeFrameIndex) {
    for (int index = 0; index < largePreviewLoaderSquares.size(); index++) {
      Region square = largePreviewLoaderSquares.get(index);
      int distance = activeFrameIndex < 0 ? -1 : Math.floorMod(index - activeFrameIndex, 4);
      double opacity =
          switch (distance) {
            case 0 -> 1.0;
            case 1 -> 0.68;
            case 2 -> 0.42;
            case 3 -> 0.24;
            default -> 0.24;
          };
      double scale =
          switch (distance) {
            case 0 -> 1.0;
            case 1 -> 0.96;
            case 2 -> 0.92;
            case 3 -> 0.88;
            default -> 0.88;
          };
      square.setOpacity(opacity);
      square.setScaleX(scale);
      square.setScaleY(scale);
    }
  }

  private void cancelLargePreviewLoadingAffordance() {
    if (largePreviewLoaderRevealTransition != null) {
      largePreviewLoaderRevealTransition.stop();
    }
    if (largePreviewLoaderAnimationTimeline != null) {
      largePreviewLoaderAnimationTimeline.stop();
    }
    if (largePreviewLoadingAffordance != null) {
      largePreviewLoadingAffordance.completeRequest();
    }
  }

  private void beginLargePreviewLoadingAffordance() {
    cancelLargePreviewLoadingAffordance();
    currentLargePreviewLoaderRequestSequence = largePreviewLoadingAffordance.beginRequest();
    largePreviewLoaderRevealTransition =
        new PauseTransition(
            javafx.util.Duration.millis(LARGE_PREVIEW_LOADER_REVEAL_DELAY.toMillis()));
    largePreviewLoaderRevealTransition.setOnFinished(
        unused -> {
          largePreviewLoadingAffordance.revealIfPending(currentLargePreviewLoaderRequestSequence);
          if (!largePreviewLoadingAffordance.visible()) {
            return;
          }
          largePreviewLoaderAnimationTimeline =
              new Timeline(
                  new KeyFrame(
                      javafx.util.Duration.millis(
                          LARGE_PREVIEW_LOADER_FRAME_INTERVAL.toMillis()),
                      event ->
                          largePreviewLoadingAffordance.advanceFrame(
                              currentLargePreviewLoaderRequestSequence)));
          largePreviewLoaderAnimationTimeline.setCycleCount(Animation.INDEFINITE);
          largePreviewLoaderAnimationTimeline.playFromStart();
        });
    largePreviewLoaderRevealTransition.playFromStart();
  }

  private void processDeferredLargePreviewScroll() {
    if (pendingLargePreviewScrollValue < 0.0) {
      return;
    }
    double deferredScrollValue = pendingLargePreviewScrollValue;
    pendingLargePreviewScrollValue = -1.0;
    setViewerScrollPosition(0.0, deferredScrollValue);
    Platform.runLater(() -> handleViewerScrollValueChanged(deferredScrollValue));
  }

  private void syncLargePreviewViewportState(
      JsonViewerLoadResult result, double targetVerticalScrollValue) {
    currentLargePreviewViewportState =
        largePreviewViewportStateResolver
            .resolveForScroll(result, targetVerticalScrollValue)
            .or(
                () ->
                    result.hasLargePreviewSession()
                        ? largePreviewViewportStateResolver.resolveForPage(
                            result, result.largePreviewSession().currentPageIndex())
                        : java.util.Optional.empty())
            .orElseGet(largePreviewViewportStateResolver::inactive);
  }

  private void clearLargePreviewViewportState() {
    currentLargePreviewViewportState = largePreviewViewportStateResolver.inactive();
  }

  private void navigateLargePreviewByStep(int pageDelta) {
    if (largePreviewPageLoadInFlight) {
      return;
    }
    if (!currentLargePreviewViewportState.active()) {
      return;
    }
    workflowService
        .currentView()
        .filter(JsonViewerLoadResult::hasLargePreviewSession)
        .flatMap(
            result ->
                largePreviewViewportStateResolver.resolveForPage(
                    result,
                    Math.max(
                        0,
                        Math.min(
                            currentLargePreviewViewportState.currentPageIndex() + pageDelta,
                            currentLargePreviewViewportState.totalPages() - 1))))
        .filter(
            targetViewportState ->
                targetViewportState.currentPageIndex()
                    != currentLargePreviewViewportState.currentPageIndex())
        .ifPresent(this::requestLargePreviewPage);
  }

  private void syncLargePreviewPageControls(JsonViewerLoadResult result) {
    LargePreviewPageNavigationState navigationState =
        result.usesLargePreview() && currentLargePreviewViewportState.active()
            ? new LargePreviewPageNavigationState(
                true,
                currentLargePreviewViewportState.currentPageNumber(),
                currentLargePreviewViewportState.totalPages(),
                currentLargePreviewViewportState.previousEnabled(),
                currentLargePreviewViewportState.nextEnabled())
            : largePreviewPageNavigationStateResolver.resolve(result);
    if (!navigationState.visible()) {
      hideLargePreviewPageControls();
      return;
    }

    largePreviewPageControls.setManaged(true);
    largePreviewPageControls.setVisible(true);
    largePreviewCurrentPageLabel.setText("Page " + navigationState.currentPageNumber());
    largePreviewTotalPagesLabel.setText("of " + navigationState.totalPages());
    largePreviewPreviousButton.setDisable(!navigationState.previousEnabled());
    largePreviewNextButton.setDisable(!navigationState.nextEnabled());
  }

  private void hideLargePreviewPageControls() {
    largePreviewPageControls.setManaged(false);
    largePreviewPageControls.setVisible(false);
    largePreviewCurrentPageLabel.setText("Page 1");
    largePreviewTotalPagesLabel.setText("of 1");
    largePreviewPreviousButton.setDisable(true);
    largePreviewNextButton.setDisable(true);
  }

  private void applyLargePreviewDocumentScrollShell(JsonViewerLoadResult result) {
    if (!result.usesLargePreview() || !result.hasLargePreviewSession()) {
      hideLargePreviewDocumentScrollShell();
      return;
    }
    result
        .largePreviewSession()
        .currentPageRange()
        .ifPresentOrElse(
            pageRange -> {
              long totalLogicalLines =
                  result.largePreviewSession().totalLogicalLines() == null
                      ? pageRange.logicalLineCount()
                      : result.largePreviewSession().totalLogicalLines();
              long topLogicalLines = pageRange.startingLogicalLine();
              long bottomLogicalLines =
                  Math.max(0L, totalLogicalLines - pageRange.endingLogicalLineExclusive());
              viewerContentBox.setSpacing(0.0);
              showLargePreviewSpacer(
                  largePreviewTopSpacer, topLogicalLines * LARGE_PREVIEW_LOGICAL_LINE_HEIGHT);
              showLargePreviewSpacer(
                  largePreviewBottomSpacer,
                  bottomLogicalLines * LARGE_PREVIEW_LOGICAL_LINE_HEIGHT);
            },
            this::hideLargePreviewDocumentScrollShell);
  }

  private void showLargePreviewSpacer(Region spacer, double height) {
    spacer.setManaged(true);
    spacer.setVisible(true);
    spacer.setMouseTransparent(true);
    spacer.setMinHeight(height);
    spacer.setPrefHeight(height);
    spacer.setMaxHeight(height);
  }

  private void hideLargePreviewDocumentScrollShell() {
    viewerContentBox.setSpacing(18.0);
    hideLargePreviewSpacer(largePreviewTopSpacer);
    hideLargePreviewSpacer(largePreviewBottomSpacer);
  }

  private void hideLargePreviewSpacer(Region spacer) {
    spacer.setManaged(false);
    spacer.setVisible(false);
    spacer.setMinHeight(0.0);
    spacer.setPrefHeight(0.0);
    spacer.setMaxHeight(0.0);
  }

  private void setViewerScrollPosition(double horizontalScrollValue, double verticalScrollValue) {
    suppressLargePreviewScrollHandling = true;
    viewerScrollPane.setHvalue(clamp(horizontalScrollValue));
    viewerScrollPane.setVvalue(clamp(verticalScrollValue));
    Platform.runLater(() -> suppressLargePreviewScrollHandling = false);
  }

  private void applyCapabilityPresentation(JsonViewerLoadResult result) {
    ViewerCapabilityPresentation presentation = capabilityPresentationResolver.resolve(result);
    copyTreeButton.setText(presentation.copyButtonText());
    copyTreeButton.setDisable(false);
    rawJsonButton.setDisable(!presentation.rawJsonEnabled());
    searchButton.setDisable(!presentation.searchEnabled());
    outlineToggleButton.setDisable(!presentation.outlineEnabled());
    setValidationBadge(
        presentation.validationBadgeText(), presentation.validationBadgeStyleClass());
    footerStatusLabel.setText(presentation.footerStatus());
    statusStateValueLabel.setText(presentation.statusState());
  }

  private void updateOutlineShell(JsonViewerLoadResult result, AsciiTreeDocument document) {
    ViewerCapabilityPresentation presentation = capabilityPresentationResolver.resolve(result);
    if (presentation.outlineEnabled()) {
      syncOutlineModelWithCurrentView();
      showOutlineValidShell(document);
      return;
    }

    resetOutlineModel();
    showOutlineShellState(
        presentation.outlineTitle(),
        presentation.outlineStateMessage(),
        presentation.outlineMetaMessage(),
        null);
  }

  private void resetToolbarForNonRenderableState() {
    copyTreeButton.setText("Copy tree");
    copyTreeButton.setDisable(true);
    rawJsonButton.setDisable(true);
    searchButton.setDisable(true);
    outlineToggleButton.setDisable(false);
  }

  private void showFileWarningIcon(boolean visible) {
    fileWarningIconLabel.setManaged(visible);
    fileWarningIconLabel.setVisible(visible);
  }

  private void syncActiveSearchStrip() {
    searchWorkflowService
        .currentSession()
        .ifPresentOrElse(
            session -> {
              activeSearchQueryLabel.setText(session.query());
              activeSearchOccurrenceLabel.setText(formatOccurrenceLabel(session));
              activeSearchStrip.setManaged(true);
              activeSearchStrip.setVisible(true);
              boolean disableNavigation = session.totalMatches() <= 1;
              previousSearchButton.setDisable(disableNavigation);
              nextSearchButton.setDisable(disableNavigation);
            },
            () -> {
              activeSearchStrip.setManaged(false);
              activeSearchStrip.setVisible(false);
              activeSearchQueryLabel.setText("Search ready");
              activeSearchOccurrenceLabel.setText("Ready");
              previousSearchButton.setDisable(true);
              nextSearchButton.setDisable(true);
            });
  }

  private String formatOccurrenceLabel(JsonSearchSession session) {
    if (!session.hasMatches()) {
      return "0 matches";
    }
    return (session.activeMatchIndex() + 1) + " / " + session.totalMatches();
  }

  private void renderRawJsonContent(String rawJson) {
    currentRawJsonPresentation = rawJsonPresentationService.present(rawJson);
    TextFlowRenderOutcome renderOutcome =
        searchTextFlowHighlighter.appendHighlightedText(
        rawJsonContentFlow,
        currentRawJsonPresentation.content(),
        currentRawHighlightRanges(),
        "raw-json-text",
        "#2d333a");
    treeContentFlow.setManaged(false);
    treeContentFlow.setVisible(false);
    rawJsonContentFlow.setManaged(true);
    rawJsonContentFlow.setVisible(true);
    emptyStateLabel.setManaged(false);
    emptyStateLabel.setVisible(false);
    rawJsonButton.setText("ASCII tree");
    showingRawJson = true;
    scheduleOutlineViewportRefresh();
    if (renderOutcome.guardrailApplied()) {
      footerStatusLabel.setText("Render budget guard active • showing simplified raw JSON");
    }
  }

  private List<SearchHighlightRange> currentAsciiHighlightRanges(AsciiTreeDocument document) {
    return searchWorkflowService
        .currentSession()
        .filter(JsonSearchSession::hasMatches)
        .map(session -> searchMatchProjector.asciiRanges(document.content(), session))
        .orElse(List.of());
  }

  private List<SearchHighlightRange> currentRawHighlightRanges() {
    return searchWorkflowService
        .currentSession()
        .filter(JsonSearchSession::hasMatches)
        .map(
            session ->
                searchMatchProjector.rawRanges(
                    session, currentRawJsonPresentation.sourceToDisplayBoundaries()))
        .orElse(List.of());
  }

  private void scrollToActiveSearchHighlight() {
    searchWorkflowService
        .currentSession()
        .filter(JsonSearchSession::hasMatches)
        .ifPresent(unused -> Platform.runLater(this::scrollActiveHighlightIntoView));
  }

  private void scrollActiveHighlightIntoView() {
    TextFlow activeFlow = showingRawJson ? rawJsonContentFlow : treeContentFlow;
    activeFlow.getChildren().stream()
        .filter(Text.class::isInstance)
        .map(Text.class::cast)
        .filter(text -> text.getStyleClass().contains("search-match-active"))
        .findFirst()
        .ifPresent(this::scrollTextIntoView);
  }

  private void scrollTextIntoView(Text textNode) {
    viewerContentBox.applyCss();
    viewerContentBox.layout();

    Bounds nodeBounds =
        viewerContentBox.sceneToLocal(textNode.localToScene(textNode.getBoundsInLocal()));
    Bounds viewportBounds = viewerScrollPane.getViewportBounds();
    Bounds contentBounds = viewerContentBox.getLayoutBounds();

    double maxHorizontalOffset =
        Math.max(1.0, contentBounds.getWidth() - viewportBounds.getWidth());
    double maxVerticalOffset =
        Math.max(1.0, contentBounds.getHeight() - viewportBounds.getHeight());

    double targetX = Math.max(0.0, nodeBounds.getMinX() - (viewportBounds.getWidth() * 0.2));
    double targetY = Math.max(0.0, nodeBounds.getMinY() - (viewportBounds.getHeight() * 0.2));

    viewerScrollPane.setHvalue(clamp(targetX / maxHorizontalOffset));
    viewerScrollPane.setVvalue(clamp(targetY / maxVerticalOffset));
  }

  private double clamp(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
