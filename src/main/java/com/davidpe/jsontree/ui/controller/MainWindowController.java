package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.model.JsonSearchExecutionResult;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import com.davidpe.jsontree.application.port.in.ImportJsonUseCase;
import com.davidpe.jsontree.application.port.out.ClipboardPort;
import com.davidpe.jsontree.application.service.JsonSearchWorkflowService;
import com.davidpe.jsontree.application.service.JsonViewerWorkflowService;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.ui.model.ViewerVisualState;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.support.AsciiTreeSyntaxHighlighter;
import com.davidpe.jsontree.ui.support.DroppedJsonPathResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class MainWindowController implements UiScreenController {

  private static final double INLINE_HISTORY_CELL_SIZE = 72.0;
  private static final int INLINE_HISTORY_MIN_VISIBLE_ROWS = 3;

  private static final DateTimeFormatter FILE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss")
          .withLocale(Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  private final AsciiTreeSyntaxHighlighter syntaxHighlighter;
  private final ImportJsonUseCase importJsonUseCase;
  private final JsonViewerWorkflowService workflowService;
  private final JsonSearchWorkflowService searchWorkflowService;
  private final ClipboardPort clipboardPort;
  private final UiFlowManager uiFlowManager;
  private final DroppedJsonPathResolver droppedJsonPathResolver;
  private final ObjectMapper objectMapper;

  public MainWindowController(
      AsciiTreeSyntaxHighlighter syntaxHighlighter,
      ImportJsonUseCase importJsonUseCase,
      JsonViewerWorkflowService workflowService,
      JsonSearchWorkflowService searchWorkflowService,
      ClipboardPort clipboardPort,
      DroppedJsonPathResolver droppedJsonPathResolver,
      ObjectMapper objectMapper,
      @Lazy UiFlowManager uiFlowManager) {
    this.syntaxHighlighter = syntaxHighlighter;
    this.importJsonUseCase = importJsonUseCase;
    this.workflowService = workflowService;
    this.searchWorkflowService = searchWorkflowService;
    this.clipboardPort = clipboardPort;
    this.droppedJsonPathResolver = droppedJsonPathResolver;
    this.objectMapper = objectMapper;
    this.uiFlowManager = uiFlowManager;
  }

  @FXML private BorderPane rootPane;

  @FXML private Label fileNameLabel;

  @FXML private Label fileMetaLabel;

  @FXML private Label fileLoadedAtValueLabel;

  @FXML private Label fileSourceValueLabel;

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

  @FXML private TextFlow treeContentFlow;

  @FXML private TextFlow rawJsonContentFlow;

  @FXML private Button rawJsonButton;

  @FXML private Button searchButton;

  @FXML private VBox searchModalCard;

  @FXML private TextField searchQueryField;

  @FXML private Label searchModalErrorLabel;

  @FXML private ListView<ImportedJsonFile> historyListView;

  @FXML private VBox outlineVBox;

  private ViewerVisualState currentState;
  private Instant currentLoadedAt;
  private String currentViewIdentity;
  private boolean windowMetricsLoggingAttached;
  private boolean showingRawJson = false;

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    configureWindowMetricsLogging();
    rootPane.setOnDragOver(this::handleDragOver);
    rootPane.setOnDragExited(event -> restoreViewFromWorkflow());
    rootPane.setOnDragDropped(this::handleDragDropped);
    historyListView.setFixedCellSize(INLINE_HISTORY_CELL_SIZE);
    double historyListHeight = (INLINE_HISTORY_CELL_SIZE * INLINE_HISTORY_MIN_VISIBLE_ROWS) + 2.0;
    historyListView.setMinHeight(historyListHeight);
    historyListView.setPrefHeight(historyListHeight);
    historyListView.setCellFactory(unused -> new InlineHistoryListCell());
    historyListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (unused, oldValue, newValue) -> {
              if (newValue == null || newValue.equals(oldValue)) {
                return;
              }
              reopenHistoryEntry(newValue);
              historyListView.getSelectionModel().clearSelection();
            });
    showEmptyViewer();
    refreshInlineHistory();
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

  @Override
  public void onShow() {
    refreshInlineHistory();
    syncActiveSearchStrip();
    restoreViewFromWorkflow();
  }

  public void renderAsciiTree(AsciiTreeDocument document) {
    resetViewModeIfNeeded();
    syntaxHighlighter.appendHighlightedContent(treeContentFlow, document);
    treeContentFlow.setManaged(true);
    treeContentFlow.setVisible(true);
    emptyStateLabel.setManaged(false);
    emptyStateLabel.setVisible(false);
    viewerAidTitleLabel.setText(document.rootLabel());
    viewerAidMetaLabel.setText(
        document.lineCount() + " rendered lines\nSource: " + fileSourceValueLabel.getText());
    setValidationBadge("Valid", "status-valid");
    footerStatusLabel.setText("Rendered " + document.lineCount() + " lines");
    statusStateValueLabel.setText("VALID");
    viewerScrollPane.setHvalue(0);
    viewerScrollPane.setVvalue(0);
    applyState(ViewerVisualState.VALID);
    rawJsonButton.setDisable(false);
    searchButton.setDisable(false);
  }

  public void showEmptyViewer() {
    currentLoadedAt = null;
    currentViewIdentity = null;
    fileNameLabel.setText("No file loaded");
    fileMetaLabel.setText("Drop a JSON anywhere in the window");
    fileLoadedAtValueLabel.setText("Not loaded");
    fileSourceValueLabel.setText("Waiting for import");
    viewerAidTitleLabel.setText("Awaiting JSON");
    viewerAidMetaLabel.setText("Load a local JSON file to populate this secondary viewer aid.");
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
    rawJsonButton.setDisable(true);
    searchButton.setDisable(true);
    clearSearchSession();
    hideSearchModal();
    resetViewModeIfNeeded();
    applyState(ViewerVisualState.EMPTY);
  }

  public void showDraggingState() {
    emptyStateLabel.setText("Release to inspect this JSON file");
    viewerAidTitleLabel.setText("Drop ready");
    viewerAidMetaLabel.setText("Release to load the first supported .json file in the payload.");
    setValidationBadge("Drop ready", "status-accent");
    footerStatusLabel.setText("Waiting for JSON drop");
    setStatusRailValues("DROP READY", "--", "--", "Drag payload");
    applyState(ViewerVisualState.DRAGGING);
  }

  public void showLoadingState(String fileName) {
    currentLoadedAt = Instant.now();
    currentViewIdentity = "loading:" + fileName;
    fileNameLabel.setText(fileName);
    fileMetaLabel.setText("Preparing JSON preview");
    fileLoadedAtValueLabel.setText(FILE_TIME_FORMATTER.format(currentLoadedAt));
    fileSourceValueLabel.setText("Local file");
    viewerAidTitleLabel.setText("Preparing outline");
    viewerAidMetaLabel.setText(
        "Workspace actions are in place while validation and rendering complete.");
    setValidationBadge("Loading", "status-muted");
    footerStatusLabel.setText("Parsing JSON");
    setStatusRailValues("LOADING", "--", "--", "Local file");
    emptyStateLabel.setText("Loading JSON preview...");
    treeContentFlow.getChildren().clear();
    treeContentFlow.setManaged(false);
    treeContentFlow.setVisible(false);
    emptyStateLabel.setManaged(true);
    emptyStateLabel.setVisible(true);
    rawJsonButton.setDisable(true);
    searchButton.setDisable(true);
    clearSearchSession();
    hideSearchModal();
    resetViewModeIfNeeded();
    applyState(ViewerVisualState.LOADING);
  }

  public void showInvalidState(String message) {
    viewerAidTitleLabel.setText("Validation issue");
    viewerAidMetaLabel.setText(
        "The viewer contract remains active even when the payload cannot render.");
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
    rawJsonButton.setDisable(true);
    searchButton.setDisable(true);
    clearSearchSession();
    hideSearchModal();
    resetViewModeIfNeeded();
    applyState(ViewerVisualState.INVALID);
  }

  public void showEmptyFileState() {
    showInvalidState("The JSON file is empty.");
    viewerAidTitleLabel.setText("Empty file");
    viewerAidMetaLabel.setText(
        "The selected file exists but does not contain any JSON content to render.");
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

    showLoadingState(jsonPath.getFileName().toString());
    JsonViewerLoadResult result =
        workflowService.loadImportedFile(importJsonUseCase.importFile(jsonPath));
    presentLoadResult(result);
    event.setDropCompleted(true);
    event.consume();
  }

  private java.util.Optional<Path> firstJsonFile(Dragboard dragboard) {
    if (!dragboard.hasFiles()) {
      return java.util.Optional.empty();
    }
    return droppedJsonPathResolver.resolve(dragboard.getFiles());
  }

  private void restoreViewFromWorkflow() {
    workflowService.currentView().ifPresentOrElse(this::presentLoadResult, this::showEmptyViewer);
  }

  private void presentLoadResult(JsonViewerLoadResult result) {
    updateFileSummary(result);
    searchWorkflowService.clearIfSourceChanged(currentViewIdentity(result));
    syncActiveSearchStrip();
    syncStatusRail(result);
    refreshInlineHistory();

    JsonValidationResult validationResult = result.validationResult();
    if (validationResult.status() == JsonValidationStatus.VALID && result.hasRenderableTree()) {
      renderAsciiTree(result.asciiTreeDocument());
      return;
    }
    if (validationResult.status() == JsonValidationStatus.EMPTY) {
      showEmptyFileState();
      return;
    }
    showInvalidState(composeValidationMessage(validationResult));
  }

  private String formatFileMeta(long sizeBytes, boolean storedInHistory) {
    String meta = formatBytes(sizeBytes);
    if (storedInHistory) {
      return meta + " • reopened from history";
    }
    return meta + " • local import";
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

  private void updateFileSummary(JsonViewerLoadResult result) {
    fileNameLabel.setText(result.importResult().fileName());
    fileMetaLabel.setText(
        formatFileMeta(result.importResult().sizeBytes(), result.historyEntry() != null));
    fileLoadedAtValueLabel.setText(FILE_TIME_FORMATTER.format(resolveLoadedAt(result)));
    fileSourceValueLabel.setText(result.historyEntry() != null ? "History snapshot" : "Local file");
  }

  private Instant resolveLoadedAt(JsonViewerLoadResult result) {
    String identity = currentViewIdentity(result);
    if (!identity.equals(currentViewIdentity)) {
      currentLoadedAt =
          result.historyEntry() != null ? result.historyEntry().importedAt() : Instant.now();
      currentViewIdentity = identity;
    }
    return currentLoadedAt;
  }

  private String currentViewIdentity(JsonViewerLoadResult result) {
    if (result.historyEntry() != null) {
      return "history:" + result.historyEntry().storedName();
    }
    return "file:" + result.importResult().path().toAbsolutePath().normalize();
  }

  private void syncStatusRail(JsonViewerLoadResult result) {
    String state =
        switch (result.validationResult().status()) {
          case VALID -> "VALID";
          case EMPTY -> "EMPTY";
          case INVALID, PARSING_ERROR -> "INVALID";
        };
    String lines =
        result.hasRenderableTree()
            ? Integer.toString(result.asciiTreeDocument().lineCount())
            : "--";
    setStatusRailValues(
        state,
        formatBytes(result.importResult().sizeBytes()),
        lines,
        result.historyEntry() != null ? "History snapshot" : "Local file");
  }

  private void setStatusRailValues(String state, String size, String lines, String source) {
    statusStateValueLabel.setText(state);
    statusSizeValueLabel.setText(size);
    statusLinesValueLabel.setText(lines);
    statusSourceValueLabel.setText(source);
  }

  private void refreshInlineHistory() {
    List<ImportedJsonFile> entries = workflowService.loadHistoryEntries();
    int visibleCount = Math.min(entries.size(), 5);
    historyInlineMetaLabel.setText(
        visibleCount == 0
            ? "No recent snapshots"
            : visibleCount + " recent snapshot" + (visibleCount == 1 ? "" : "s"));

    if (entries.isEmpty()) {
      historyListView.getItems().clear();
      historyListView.setManaged(false);
      historyListView.setVisible(false);
      emptyHistoryInlineLabel.setManaged(true);
      emptyHistoryInlineLabel.setVisible(true);
      return;
    }

    emptyHistoryInlineLabel.setManaged(false);
    emptyHistoryInlineLabel.setVisible(false);
    historyListView.setManaged(true);
    historyListView.setVisible(true);
    historyListView.getItems().setAll(entries.stream().limit(5).toList());
  }

  private void reopenHistoryEntry(ImportedJsonFile entry) {
    workflowService.reopenHistoryEntry(entry.storedName()).ifPresent(this::presentLoadResult);
  }

  private final class InlineHistoryListCell extends ListCell<ImportedJsonFile> {

    private final Label titleLabel = new Label();
    private final Label metaLabel = new Label();
    private final VBox content = new VBox(4.0);

    private InlineHistoryListCell() {
      titleLabel.getStyleClass().add("history-inline-title");
      metaLabel.getStyleClass().add("history-inline-meta");
      content.getChildren().addAll(titleLabel, metaLabel);
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
    if (showingRawJson) {
      rawJsonContentFlow.getChildren().clear();
      rawJsonContentFlow.setManaged(false);
      rawJsonContentFlow.setVisible(false);
      treeContentFlow.setManaged(true);
      treeContentFlow.setVisible(true);
      rawJsonButton.setText("Raw JSON");
      showingRawJson = false;
    } else {
      String formatted = workflowService.currentViewRawJson().map(this::prettyPrint).orElse("");
      Text jsonText = new Text(formatted);
      jsonText.getStyleClass().add("raw-json-text");
      rawJsonContentFlow.getChildren().clear();
      rawJsonContentFlow.getChildren().add(jsonText);
      treeContentFlow.setManaged(false);
      treeContentFlow.setVisible(false);
      rawJsonContentFlow.setManaged(true);
      rawJsonContentFlow.setVisible(true);
      rawJsonButton.setText("ASCII tree");
      showingRawJson = true;
    }
    viewerScrollPane.setHvalue(0);
    viewerScrollPane.setVvalue(0);
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
  }

  private String prettyPrint(String rawJson) {
    try {
      return objectMapper
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(objectMapper.readTree(rawJson));
    } catch (JsonProcessingException e) {
      return rawJson;
    }
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
  void openSearchModal() {
    searchQueryField.setText(searchWorkflowService.currentSession().map(JsonSearchSession::query).orElse(""));
    searchModalErrorLabel.setManaged(false);
    searchModalErrorLabel.setVisible(false);
    searchModalErrorLabel.setText("");
    searchModalCard.setManaged(true);
    searchModalCard.setVisible(true);
    Platform.runLater(() -> {
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
  }

  @FXML
  void clearSearchSession() {
    searchWorkflowService.clear();
    syncActiveSearchStrip();
  }

  @FXML
  void showPreviousSearchResult() {
    // Navigation wiring lands in the search workflow tickets.
  }

  @FXML
  void showNextSearchResult() {
    // Navigation wiring lands in the search workflow tickets.
  }

  @FXML
  void copyTree() {
    workflowService.currentViewRawJson().ifPresent(clipboardPort::copy);
  }

  @FXML
  void toggleOutline() {
    boolean nextVisible = !outlineVBox.isVisible();
    outlineVBox.setVisible(nextVisible);
    outlineVBox.setManaged(nextVisible);
  }

  private void hideSearchModal() {
    searchModalCard.setManaged(false);
    searchModalCard.setVisible(false);
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
            },
            () -> {
              activeSearchStrip.setManaged(false);
              activeSearchStrip.setVisible(false);
              activeSearchQueryLabel.setText("Search ready");
              activeSearchOccurrenceLabel.setText("Ready");
            });
  }

  private String formatOccurrenceLabel(JsonSearchSession session) {
    if (!session.hasMatches()) {
      return "0 matches";
    }
    return (session.activeMatchIndex() + 1) + " / " + session.totalMatches();
  }
}
