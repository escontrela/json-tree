package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.port.in.ImportJsonUseCase;
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
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class MainWindowController implements UiScreenController {

  private static final DateTimeFormatter FILE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss")
          .withLocale(Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  private final AsciiTreeSyntaxHighlighter syntaxHighlighter;
  private final ImportJsonUseCase importJsonUseCase;
  private final JsonViewerWorkflowService workflowService;
  private final UiFlowManager uiFlowManager;
  private final DroppedJsonPathResolver droppedJsonPathResolver;

  public MainWindowController(
      AsciiTreeSyntaxHighlighter syntaxHighlighter,
      ImportJsonUseCase importJsonUseCase,
      JsonViewerWorkflowService workflowService,
      DroppedJsonPathResolver droppedJsonPathResolver,
      @Lazy UiFlowManager uiFlowManager) {
    this.syntaxHighlighter = syntaxHighlighter;
    this.importJsonUseCase = importJsonUseCase;
    this.workflowService = workflowService;
    this.droppedJsonPathResolver = droppedJsonPathResolver;
    this.uiFlowManager = uiFlowManager;
  }

  @FXML private BorderPane rootPane;

  @FXML private Label fileNameLabel;

  @FXML private Label fileMetaLabel;

  @FXML private Label fileLoadedAtValueLabel;

  @FXML private Label fileTypeValueLabel;

  @FXML private Label fileLinesValueLabel;

  @FXML private Label fileSourceValueLabel;

  @FXML private Label validationStatusLabel;

  @FXML private Label historyInlineMetaLabel;

  @FXML private Label emptyHistoryInlineLabel;

  @FXML private Label importUtilityTitleLabel;

  @FXML private Label importUtilitySupportLabel;

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

  @FXML private VBox importUtilityCard;

  @FXML private VBox viewerContentBox;

  @FXML private TextFlow treeContentFlow;

  @FXML private ListView<ImportedJsonFile> historyListView;

  private ViewerVisualState currentState;
  private Instant currentLoadedAt;
  private String currentViewIdentity;

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    rootPane.setOnDragOver(this::handleDragOver);
    rootPane.setOnDragExited(event -> restoreViewFromWorkflow());
    rootPane.setOnDragDropped(this::handleDragDropped);
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

  @Override
  public void onShow() {
    refreshInlineHistory();
    restoreViewFromWorkflow();
  }

  public void renderAsciiTree(AsciiTreeDocument document) {
    syntaxHighlighter.appendHighlightedContent(treeContentFlow, document);
    treeContentFlow.setManaged(true);
    treeContentFlow.setVisible(true);
    emptyStateLabel.setManaged(false);
    emptyStateLabel.setVisible(false);
    viewerAidTitleLabel.setText(document.rootLabel());
    viewerAidMetaLabel.setText(
        document.lineCount() + " rendered lines\nSource: " + fileSourceValueLabel.getText());
    importUtilityTitleLabel.setText("Import another JSON");
    importUtilitySupportLabel.setText(
        "Drop a new .json anywhere in the window or reopen one of the recent snapshots from the"
            + " rail.");
    setValidationBadge("Valid", "status-valid");
    footerStatusLabel.setText("Rendered " + document.lineCount() + " lines");
    statusStateValueLabel.setText("VALID");
    viewerScrollPane.setHvalue(0);
    viewerScrollPane.setVvalue(0);
    applyState(ViewerVisualState.VALID);
  }

  public void showEmptyViewer() {
    currentLoadedAt = null;
    currentViewIdentity = null;
    fileNameLabel.setText("No file loaded");
    fileMetaLabel.setText("Drop a JSON anywhere in the window");
    fileLoadedAtValueLabel.setText("Not loaded");
    fileTypeValueLabel.setText("application/json");
    fileLinesValueLabel.setText("--");
    fileSourceValueLabel.setText("Waiting for import");
    viewerAidTitleLabel.setText("Awaiting JSON");
    viewerAidMetaLabel.setText("Load a local JSON file to populate this secondary viewer aid.");
    importUtilityTitleLabel.setText("Drop JSON anywhere in this window");
    importUtilitySupportLabel.setText(
        "The full window remains the drop target. Valid JSON snapshots will also appear in the"
            + " recent history rail.");
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
    applyState(ViewerVisualState.EMPTY);
  }

  public void showDraggingState() {
    emptyStateLabel.setText("Release to inspect this JSON file");
    viewerAidTitleLabel.setText("Drop ready");
    viewerAidMetaLabel.setText("Release to load the first supported .json file in the payload.");
    importUtilityTitleLabel.setText("Release to inspect this JSON");
    importUtilitySupportLabel.setText(
        "The first supported .json file in the drop payload will enter the standard import and"
            + " validation flow.");
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
    fileTypeValueLabel.setText(detectContentType(fileName));
    fileLinesValueLabel.setText("--");
    fileSourceValueLabel.setText("Local file");
    viewerAidTitleLabel.setText("Preparing outline");
    viewerAidMetaLabel.setText(
        "Workspace actions are in place while validation and rendering complete.");
    importUtilityTitleLabel.setText("Importing JSON");
    importUtilitySupportLabel.setText(
        "Running validation, tree rendering, and local history persistence.");
    setValidationBadge("Loading", "status-muted");
    footerStatusLabel.setText("Parsing JSON");
    setStatusRailValues("LOADING", "--", "--", "Local file");
    emptyStateLabel.setText("Loading JSON preview...");
    treeContentFlow.getChildren().clear();
    treeContentFlow.setManaged(false);
    treeContentFlow.setVisible(false);
    emptyStateLabel.setManaged(true);
    emptyStateLabel.setVisible(true);
    applyState(ViewerVisualState.LOADING);
  }

  public void showInvalidState(String message) {
    viewerAidTitleLabel.setText("Validation issue");
    viewerAidMetaLabel.setText(
        "The viewer contract remains active even when the payload cannot render.");
    importUtilityTitleLabel.setText("JSON needs attention");
    importUtilitySupportLabel.setText(
        "Fix the payload and drop it again, or reopen a clean snapshot from the recent history"
            + " list.");
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
    importUtilityCard
        .getStyleClass()
        .removeAll("utility-dragging", "utility-loading", "utility-valid", "utility-invalid");
    switch (state) {
      case DRAGGING -> {
        viewerShell.getStyleClass().add("viewer-dragging");
        statusRail.getStyleClass().add("shell-dragging");
        fileSummaryCard.getStyleClass().add("shell-dragging");
        importUtilityCard.getStyleClass().add("utility-dragging");
      }
      case LOADING -> {
        viewerShell.getStyleClass().add("viewer-loading");
        statusRail.getStyleClass().add("shell-loading");
        fileSummaryCard.getStyleClass().add("shell-loading");
        importUtilityCard.getStyleClass().add("utility-loading");
      }
      case VALID -> {
        viewerShell.getStyleClass().add("viewer-valid");
        statusRail.getStyleClass().add("shell-valid");
        fileSummaryCard.getStyleClass().add("shell-valid");
        importUtilityCard.getStyleClass().add("utility-valid");
      }
      case INVALID -> {
        viewerShell.getStyleClass().add("viewer-invalid");
        statusRail.getStyleClass().add("shell-invalid");
        fileSummaryCard.getStyleClass().add("shell-invalid");
        importUtilityCard.getStyleClass().add("utility-invalid");
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
    fileTypeValueLabel.setText(detectContentType(result.importResult().fileName()));
    fileLinesValueLabel.setText(
        result.hasRenderableTree()
            ? Integer.toString(result.asciiTreeDocument().lineCount())
            : "--");
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

  private String detectContentType(String fileName) {
    return fileName.toLowerCase(Locale.ROOT).endsWith(".json") ? "application/json" : "Unknown";
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
}
