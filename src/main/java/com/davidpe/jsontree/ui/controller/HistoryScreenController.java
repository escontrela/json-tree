package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.port.in.ToggleHistoryFavoriteUseCase;
import com.davidpe.jsontree.application.service.JsonViewerWorkflowService;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.support.HistoryFavoritePresentation;
import com.davidpe.jsontree.ui.support.HistoryFavoritePresentationResolver;
import com.davidpe.jsontree.ui.support.HistoryFavoritesViewState;
import com.davidpe.jsontree.ui.support.HistoryFavoritesViewStateResolver;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
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
  private final ToggleHistoryFavoriteUseCase toggleHistoryFavoriteUseCase;
  private final HistoryFavoritePresentationResolver historyFavoritePresentationResolver;
  private final HistoryFavoritesViewStateResolver historyFavoritesViewStateResolver;
  private final UiFlowManager uiFlowManager;
  private boolean favoritesOnly;

  public HistoryScreenController(
      JsonViewerWorkflowService workflowService,
      ToggleHistoryFavoriteUseCase toggleHistoryFavoriteUseCase,
      HistoryFavoritePresentationResolver historyFavoritePresentationResolver,
      HistoryFavoritesViewStateResolver historyFavoritesViewStateResolver,
      @Lazy UiFlowManager uiFlowManager) {
    this.workflowService = workflowService;
    this.toggleHistoryFavoriteUseCase = toggleHistoryFavoriteUseCase;
    this.historyFavoritePresentationResolver = historyFavoritePresentationResolver;
    this.historyFavoritesViewStateResolver = historyFavoritesViewStateResolver;
    this.uiFlowManager = uiFlowManager;
  }

  @FXML private BorderPane rootPane;

  @FXML private Label historyMetaLabel;

  @FXML private Label emptyHistoryLabel;

  @FXML private ListView<ImportedJsonFile> historyListView;

  @FXML private HBox storedInspectionsRegion;

  @FXML private Button favoritesFilterButton;

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    historyListView.setCellFactory(unused -> new HistoryEntryListCell());
    applyFavoritesFilterButtonStyle(false);
  }

  @Override
  public void onShow() {
    List<ImportedJsonFile> entries = workflowService.loadHistoryEntries();
    HistoryFavoritesViewState viewState =
        historyFavoritesViewStateResolver.resolve(entries, favoritesOnly);
    historyMetaLabel.setText(viewState.summaryLabel());
    favoritesFilterButton.setText(viewState.toggleButtonText());
    applyFavoritesFilterButtonStyle(viewState.favoritesOnly());

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
    return String.format(Locale.ROOT, "%.1f %cB", scaled, iterator.current());
  }

  private final class HistoryEntryListCell extends ListCell<ImportedJsonFile> {

    private final Label titleLabel = new Label();
    private final Label metaLabel = new Label();
    private final Button favoriteButton = new Button("Pin");
    private final Button deleteButton = new Button("Delete");
    private final VBox textBox = new VBox(4.0);
    private final Region spacer = new Region();
    private final HBox content = new HBox(12.0);

    private HistoryEntryListCell() {
      titleLabel.getStyleClass().add("history-entry-title");
      metaLabel.getStyleClass().add("history-entry-meta");
      favoriteButton.getStyleClass().addAll("ghost-button", "history-favorite-button");
      favoriteButton.setOnAction(
          event -> {
            ImportedJsonFile item = getItem();
            if (item != null) {
              toggleFavoriteEntry(item);
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

      textBox.getChildren().addAll(titleLabel, metaLabel);
      HBox.setHgrow(spacer, Priority.ALWAYS);
      content.setAlignment(Pos.CENTER_LEFT);
      content.getChildren().addAll(textBox, spacer, favoriteButton, deleteButton);

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
      metaLabel.setText(
          HISTORY_TIME_FORMATTER.format(item.importedAt())
              + " • "
              + formatBytes(item.sizeBytes())
              + " • "
              + (item.valid() ? "VALID" : "INVALID"));
      HistoryFavoritePresentation favoritePresentation =
          historyFavoritePresentationResolver.resolve(item);
      titleLabel.setText(favoritePresentation.title());
      favoriteButton.setText(favoritePresentation.buttonText());
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
        if (current == deleteButton || current == favoriteButton) {
          return true;
        }
        current = current.getParent();
      }
      return false;
    }
  }
}
