package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.service.JsonViewerWorkflowService;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.support.ControllerAwareBorderPane;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
            JsonViewerWorkflowService workflowService,
            @Lazy UiFlowManager uiFlowManager
    ) {
        this.workflowService = workflowService;
        this.uiFlowManager = uiFlowManager;
    }

    @FXML
    private ControllerAwareBorderPane rootPane;

    @FXML
    private Label historyMetaLabel;

    @FXML
    private Label historyStatusLabel;

    @FXML
    private Label emptyHistoryLabel;

    @FXML
    private Label historyFooterLabel;

    @FXML
    private ListView<ImportedJsonFile> historyListView;

    @FXML
    public void initialize() {
        rootPane.attachController(this);
        historyListView.setCellFactory(unused -> new HistoryEntryListCell());
    }

    @Override
    public void onShow() {
        List<ImportedJsonFile> entries = workflowService.loadHistoryEntries();
        historyMetaLabel.setText(entries.size() + " stored snapshot" + (entries.size() == 1 ? "" : "s"));
        historyStatusLabel.setText(entries.isEmpty() ? "Empty" : "Ready");
        historyFooterLabel.setText(entries.isEmpty() ? "No snapshots stored yet" : "Browsing local JSON history");

        if (entries.isEmpty()) {
            historyListView.getItems().clear();
            historyListView.setManaged(false);
            historyListView.setVisible(false);
            emptyHistoryLabel.setManaged(true);
            emptyHistoryLabel.setVisible(true);
            emptyHistoryLabel.setText("No JSON snapshots yet.\nDrop a valid JSON in the main view to start building history.");
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

        @Override
        protected void updateItem(ImportedJsonFile item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }
            setText(
                    item.originalName() + "\n"
                            + HISTORY_TIME_FORMATTER.format(item.importedAt()) + " • "
                            + formatBytes(item.sizeBytes()) + " • "
                            + (item.valid() ? "VALID" : "INVALID")
            );
        }
    }
}
