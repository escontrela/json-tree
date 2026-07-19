package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.HistoryJsonImportResult;
import com.davidpe.jsontree.application.model.HistoryJsonImportStatus;
import com.davidpe.jsontree.application.model.HistoryJsonSearchResult;
import com.davidpe.jsontree.application.port.in.ImportHistoryJsonUseCase;
import com.davidpe.jsontree.application.port.in.SearchHistoryJsonUseCase;
import com.davidpe.jsontree.application.port.in.ToggleHistoryFavoriteUseCase;
import com.davidpe.jsontree.application.service.JsonViewerWorkflowService;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.ui.support.HistoryArchiveViewState;
import com.davidpe.jsontree.ui.support.HistoryArchiveViewStateResolver;
import com.davidpe.jsontree.ui.support.HistoryCurlEditAvailabilityResolver;
import com.davidpe.jsontree.ui.support.HistoryFavoritePresentation;
import com.davidpe.jsontree.ui.support.HistoryFavoritePresentationResolver;
import com.davidpe.jsontree.ui.support.LargePreviewIndicatorResolver;
import com.davidpe.jsontree.ui.support.LargePreviewWarningIconFactory;
import com.davidpe.jsontree.ui.support.ByteSizeFormatter;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.service.CurlEditorModalCoordinator;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class HistoryScreenController implements UiScreenController {

  private static final DateTimeFormatter HISTORY_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
          .withLocale(Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  private final JsonViewerWorkflowService workflowService;
  private final ImportHistoryJsonUseCase importHistoryJsonUseCase;
  private final SearchHistoryJsonUseCase searchHistoryJsonUseCase;
  private final ToggleHistoryFavoriteUseCase toggleHistoryFavoriteUseCase;
  private final HistoryFavoritePresentationResolver historyFavoritePresentationResolver;
  private final HistoryArchiveViewStateResolver historyArchiveViewStateResolver;
  private final HistoryCurlEditAvailabilityResolver historyCurlEditAvailabilityResolver;
  private final LargePreviewIndicatorResolver largePreviewIndicatorResolver;
  private final CurlEditorModalCoordinator curlEditorModalCoordinator;
  private final UiFlowManager uiFlowManager;
  private boolean favoritesOnly;
  private String activeSearchQuery = "";

  public HistoryScreenController(
      JsonViewerWorkflowService workflowService,
      ImportHistoryJsonUseCase importHistoryJsonUseCase,
      SearchHistoryJsonUseCase searchHistoryJsonUseCase,
      ToggleHistoryFavoriteUseCase toggleHistoryFavoriteUseCase,
      HistoryFavoritePresentationResolver historyFavoritePresentationResolver,
      HistoryArchiveViewStateResolver historyArchiveViewStateResolver,
      HistoryCurlEditAvailabilityResolver historyCurlEditAvailabilityResolver,
      LargePreviewIndicatorResolver largePreviewIndicatorResolver,
      CurlEditorModalCoordinator curlEditorModalCoordinator,
      @Lazy UiFlowManager uiFlowManager) {
    this.workflowService = workflowService;
    this.importHistoryJsonUseCase = importHistoryJsonUseCase;
    this.searchHistoryJsonUseCase = searchHistoryJsonUseCase;
    this.toggleHistoryFavoriteUseCase = toggleHistoryFavoriteUseCase;
    this.historyFavoritePresentationResolver = historyFavoritePresentationResolver;
    this.historyArchiveViewStateResolver = historyArchiveViewStateResolver;
    this.historyCurlEditAvailabilityResolver = historyCurlEditAvailabilityResolver;
    this.largePreviewIndicatorResolver = largePreviewIndicatorResolver;
    this.curlEditorModalCoordinator = curlEditorModalCoordinator;
    this.uiFlowManager = uiFlowManager;
  }

  @FXML private BorderPane rootPane;

  @FXML private Label historyMetaLabel;

  @FXML private Label emptyHistoryLabel;

  @FXML private ListView<ImportedJsonFile> historyListView;

  @FXML private HBox storedInspectionsRegion;

  @FXML private HBox historySearchControlsBox;

  @FXML private TextField historySearchField;

  @FXML private Button historySearchButton;

  @FXML private Button favoritesFilterButton;

  @FXML private Button importJsonButton;

  @FXML private Button newCurlButton;

  @FXML private Label historyImportFeedbackLabel;

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    historyListView.setCellFactory(unused -> new HistoryEntryListCell());
    applyFavoritesFilterButtonStyle(false);
    hideImportFeedback();
    historySearchField
        .textProperty()
        .addListener(
            (unused, oldValue, newValue) -> {
              if (favoritesOnly || activeSearchQuery.isBlank()) {
                return;
              }
              if (newValue == null || newValue.isBlank()) {
                activeSearchQuery = "";
                onShow();
              }
            });
  }

  @Override
  public void onShow() {
    List<ImportedJsonFile> entries = workflowService.loadHistoryEntries();
    if (favoritesOnly) {
      activeSearchQuery = "";
    }
    HistoryJsonSearchResult searchResult =
        activeSearchQuery.isBlank()
            ? HistoryJsonSearchResult.cleared(entries)
            : searchHistoryJsonUseCase.search(activeSearchQuery, true);
    HistoryArchiveViewState viewState =
        historyArchiveViewStateResolver.resolve(entries, favoritesOnly, searchResult);
    historyMetaLabel.setText(viewState.summaryLabel());
    favoritesFilterButton.setText(viewState.toggleButtonText());
    applyFavoritesFilterButtonStyle(viewState.favoritesOnly());
    syncImportButtonVisibility(viewState.favoritesOnly());
    syncSearchControls(viewState.favoritesOnly());

    if (viewState.visibleEntries().isEmpty()) {
      historyListView.getItems().clear();
      historyListView.setManaged(false);
      historyListView.setVisible(false);
      emptyHistoryLabel.setManaged(true);
      emptyHistoryLabel.setVisible(true);
      emptyHistoryLabel.setText(viewState.emptyMessage());
      return;
    }

    emptyHistoryLabel.setManaged(false);
    emptyHistoryLabel.setVisible(false);
    historyListView.setManaged(true);
    historyListView.setVisible(true);
    historyListView.getItems().setAll(viewState.visibleEntries());
  }

  @FXML
  void backToMain() {
    uiFlowManager.show(UiScreenId.MAIN);
  }

  @FXML
  void toggleFavoritesOnly() {
    favoritesOnly = !favoritesOnly;
    onShow();
  }

  @FXML
  void executeHistorySearch() {
    HistoryJsonSearchResult result =
        searchHistoryJsonUseCase.search(historySearchField.getText(), !favoritesOnly);
    if (result.blocked()) {
      return;
    }
    activeSearchQuery = result.searchActive() ? result.query() : "";
    onShow();
  }

  @FXML
  void importJson() {
    HistoryJsonImportResult result = importHistoryJsonUseCase.importFromDisk();
    if (result.status() == HistoryJsonImportStatus.CANCELLED) {
      return;
    }

    if (result.successful()) {
      showImportFeedback(
          "Imported " + result.importedEntry().originalName() + " into history.",
          false);
      onShow();
      historyListView.getSelectionModel().select(result.importedEntry());
      historyListView.scrollTo(result.importedEntry());
      return;
    }

    showImportFeedback(result.message(), true);
    onShow();
  }

  @FXML
  void openNewCurl() {
    curlEditorModalCoordinator.openNew(this::onShow);
  }

  private void applyFavoritesFilterButtonStyle(boolean active) {
    ObservableList<String> styleClasses = favoritesFilterButton.getStyleClass();
    if (!styleClasses.contains("ghost-button")) {
      styleClasses.add("ghost-button");
    }
    if (!styleClasses.contains("history-filter-button")) {
      styleClasses.add("history-filter-button");
    }
    styleClasses.removeAll("history-filter-button-active", "primary-button");
    if (active) {
      styleClasses.addAll("primary-button", "history-filter-button-active");
    }
  }

  private void syncImportButtonVisibility(boolean favoritesOnlyActive) {
    importJsonButton.setManaged(!favoritesOnlyActive);
    importJsonButton.setVisible(!favoritesOnlyActive);
  }

  private void syncSearchControls(boolean favoritesOnlyActive) {
    historySearchControlsBox.setManaged(!favoritesOnlyActive);
    historySearchControlsBox.setVisible(!favoritesOnlyActive);
    historySearchButton.setDisable(favoritesOnlyActive);
    if (favoritesOnlyActive) {
      historySearchField.clear();
      return;
    }
    if (!historySearchField.getText().equals(activeSearchQuery)) {
      historySearchField.setText(activeSearchQuery);
    }
  }

  private void showImportFeedback(String message, boolean error) {
    historyImportFeedbackLabel.setText(message);
    historyImportFeedbackLabel.setManaged(true);
    historyImportFeedbackLabel.setVisible(true);
    ObservableList<String> styleClasses = historyImportFeedbackLabel.getStyleClass();
    styleClasses.remove("history-import-feedback-error");
    if (error) {
      styleClasses.add("history-import-feedback-error");
    }
  }

  private void hideImportFeedback() {
    historyImportFeedbackLabel.setManaged(false);
    historyImportFeedbackLabel.setVisible(false);
    historyImportFeedbackLabel.getStyleClass().remove("history-import-feedback-error");
  }

  private void reopenEntry(ImportedJsonFile entry) {
    workflowService
        .reopenHistoryEntry(entry.storedName())
        .ifPresent(unused -> uiFlowManager.show(UiScreenId.MAIN));
  }

  private void deleteEntry(ImportedJsonFile entry) {
    workflowService.deleteHistoryEntry(entry.storedName());
    onShow();
  }

  private void toggleFavoriteEntry(ImportedJsonFile entry) {
    if (!toggleHistoryFavoriteUseCase.toggleFavorite(entry.storedName()).found()) {
      onShow();
      return;
    }
    onShow();
  }

  void openEditCurl(ImportedJsonFile entry) {
    if (!historyCurlEditAvailabilityResolver.supports(entry)) {
      return;
    }
    curlEditorModalCoordinator.openPrefilled(entry.curlCommand(), this::onShow);
  }

  private final class HistoryEntryListCell extends ListCell<ImportedJsonFile> {

    private final Label titleLabel = new Label();
    private final Label metaLabel = new Label();
    private final javafx.scene.image.ImageView warningIcon =
        LargePreviewWarningIconFactory.create(14.0);
    private final Button favoriteButton = new Button("Pin");
    private final Button editCurlButton = new Button("Edit/Rerun curl");
    private final Button deleteButton = new Button("Delete");
    private final HBox titleRow = new HBox(8.0);
    private final VBox textBox = new VBox(4.0);
    private final Region spacer = new Region();
    private final HBox content = new HBox(12.0);

    private HistoryEntryListCell() {
      titleLabel.getStyleClass().add("history-entry-title");
      metaLabel.getStyleClass().add("history-entry-meta");
      warningIcon.setManaged(false);
      warningIcon.setVisible(false);
      favoriteButton.getStyleClass().addAll("ghost-button", "history-favorite-button");
      favoriteButton.setOnAction(
          event -> {
            ImportedJsonFile item = getItem();
            if (item != null) {
              toggleFavoriteEntry(item);
            }
            event.consume();
          });
      editCurlButton.getStyleClass().addAll("ghost-button", "history-curl-button");
      editCurlButton.setManaged(false);
      editCurlButton.setVisible(false);
      editCurlButton.setOnAction(
          event -> {
            ImportedJsonFile item = getItem();
            if (item != null) {
              openEditCurl(item);
            }
            event.consume();
          });
      deleteButton.getStyleClass().add("ghost-button");
      deleteButton.getStyleClass().add("history-delete-button");
      deleteButton.setOnAction(
          event -> {
            ImportedJsonFile item = getItem();
            if (item != null) {
              deleteEntry(item);
            }
            event.consume();
          });

      titleRow.getChildren().addAll(titleLabel, warningIcon);
      textBox.getChildren().addAll(titleRow, metaLabel);
      HBox.setHgrow(spacer, Priority.ALWAYS);
      content.setAlignment(Pos.CENTER_LEFT);
      content.getChildren().addAll(textBox, spacer, editCurlButton, favoriteButton, deleteButton);

      setOnMouseClicked(
          event -> {
            if (event.getButton() == MouseButton.PRIMARY
                && !isEmpty()
                && !isDeleteInteraction(event.getTarget())) {
              reopenEntry(getItem());
            }
          });
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
          HISTORY_TIME_FORMATTER.format(item.importedAt())
              + " • "
              + ByteSizeFormatter.format(item.sizeBytes())
              + " • "
              + historyDocumentLabel(item)
              + " • "
              + item.lineCount()
              + " lines");
      HistoryFavoritePresentation favoritePresentation =
          historyFavoritePresentationResolver.resolve(item);
      boolean editableCurl = historyCurlEditAvailabilityResolver.supports(item);
      titleLabel.setText(favoritePresentation.title());
      favoriteButton.setText(favoritePresentation.buttonText());
      editCurlButton.setManaged(editableCurl);
      editCurlButton.setVisible(editableCurl);
      favoriteButton.getStyleClass().removeAll("history-favorite-button-active", "primary-button");
      titleLabel.getStyleClass().remove("history-entry-title-favorite");
      if (favoritePresentation.active()) {
        favoriteButton.getStyleClass().addAll("primary-button", "history-favorite-button-active");
        titleLabel.getStyleClass().add("history-entry-title-favorite");
      }
      setText(null);
      setGraphic(content);
    }

    private boolean isDeleteInteraction(Object target) {
      if (!(target instanceof Node node)) {
        return false;
      }
      Node current = node;
      while (current != null) {
        if (current == deleteButton || current == favoriteButton || current == editCurlButton) {
          return true;
        }
        current = current.getParent();
      }
      return false;
    }
  }

  private String historyDocumentLabel(ImportedJsonFile item) {
    String baseLabel;
    if (item.documentFormat().markdown()) {
      baseLabel = "Markdown";
    } else {
      baseLabel = item.valid() ? "JSON" : "Invalid JSON";
    }
    return item.curlBacked() ? baseLabel + " • curl fetch" : baseLabel;
  }
}
