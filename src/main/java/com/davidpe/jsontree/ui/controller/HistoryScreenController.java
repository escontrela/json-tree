package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.service.JsonViewerWorkflowService;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
  private final UiFlowManager uiFlowManager;

  public HistoryScreenController(
      JsonViewerWorkflowService workflowService, @Lazy UiFlowManager uiFlowManager) {
    this.workflowService = workflowService;
    this.uiFlowManager = uiFlowManager;
  }

  @FXML private BorderPane rootPane;

  @FXML private Label historyMetaLabel;

  @FXML private Label emptyHistoryLabel;

  @FXML private Label historyFooterLabel;

  @FXML private ListView<ImportedJsonFile> historyListView;

  @FXML private Region storedInspectionsRegion;

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    historyListView.setCellFactory(unused -> new HistoryEntryListCell());
  }

  @Override
  public void onShow() {
    List<ImportedJsonFile> entries = workflowService.loadHistoryEntries();
    historyMetaLabel.setText(
        entries.size() + " stored snapshot" + (entries.size() == 1 ? "" : "s"));
    historyFooterLabel.setText(
        entries.isEmpty() ? "No snapshots stored yet" : "Browsing local JSON history");

    if (entries.isEmpty()) {
      historyListView.getItems().clear();
      historyListView.setManaged(false);
      historyListView.setVisible(false);
      emptyHistoryLabel.setManaged(true);
      emptyHistoryLabel.setVisible(true);
      emptyHistoryLabel.setText(
          "No JSON snapshots yet.\nDrop a valid JSON in the main view to start building history.");
      return;
    }

    emptyHistoryLabel.setManaged(false);
    emptyHistoryLabel.setVisible(false);
    historyListView.setManaged(true);
    historyListView.setVisible(true);
    historyListView.getItems().setAll(entries);
  }

  @FXML
  void backToMain() {
    uiFlowManager.show(UiScreenId.MAIN);
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
    private final Button deleteButton = new Button("Delete");
    private final VBox textBox = new VBox(4.0);
    private final Region spacer = new Region();
    private final HBox content = new HBox(12.0);

    private HistoryEntryListCell() {
      titleLabel.getStyleClass().add("history-entry-title");
      metaLabel.getStyleClass().add("history-entry-meta");
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
      content.getChildren().addAll(textBox, spacer, deleteButton);

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
      setText(null);
      setGraphic(content);
    }

    private boolean isDeleteInteraction(Object target) {
      if (!(target instanceof Node node)) {
        return false;
      }
      Node current = node;
      while (current != null) {
        if (current == deleteButton) {
          return true;
        }
        current = current.getParent();
      }
      return false;
    }
  }
}
