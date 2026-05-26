package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.service.JsonViewerWorkflowService;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.ui.model.ViewerVisualState;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.support.AsciiTreeSyntaxHighlighter;
import com.davidpe.jsontree.ui.support.ControllerAwareBorderPane;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import javafx.fxml.FXML;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class MainWindowController implements UiScreenController {

    private final AsciiTreeSyntaxHighlighter syntaxHighlighter;
    private final JsonViewerWorkflowService workflowService;
    private final UiFlowManager uiFlowManager;

    public MainWindowController(
            AsciiTreeSyntaxHighlighter syntaxHighlighter,
            JsonViewerWorkflowService workflowService,
            @Lazy UiFlowManager uiFlowManager
    ) {
        this.syntaxHighlighter = syntaxHighlighter;
        this.workflowService = workflowService;
        this.uiFlowManager = uiFlowManager;
    }

    @FXML
    private ControllerAwareBorderPane rootPane;

    @FXML
    private Label fileNameLabel;

    @FXML
    private Label fileMetaLabel;

    @FXML
    private Label validationStatusLabel;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private Label footerStatusLabel;

    @FXML
    private ScrollPane viewerScrollPane;

    @FXML
    private StackPane viewerShell;

    @FXML
    private VBox viewerContentBox;

    @FXML
    private TextFlow treeContentFlow;

    private ViewerVisualState currentState;

    @FXML
    public void initialize() {
        rootPane.attachController(this);
        fileNameLabel.setText("No file loaded");
        fileMetaLabel.setText("Drop a JSON anywhere in the window");
        rootPane.setOnDragOver(this::handleDragOver);
        rootPane.setOnDragExited(event -> restoreViewFromWorkflow());
        rootPane.setOnDragDropped(this::handleDragDropped);
        showEmptyViewer();
    }

    @Override
    public void onShow() {
        restoreViewFromWorkflow();
    }

    public void renderAsciiTree(AsciiTreeDocument document) {
        syntaxHighlighter.appendHighlightedContent(treeContentFlow, document);
        treeContentFlow.setManaged(true);
        treeContentFlow.setVisible(true);
        emptyStateLabel.setManaged(false);
        emptyStateLabel.setVisible(false);
        validationStatusLabel.setText("Valid");
        footerStatusLabel.setText("Rendered " + document.lineCount() + " lines");
        viewerScrollPane.setHvalue(0);
        viewerScrollPane.setVvalue(0);
        applyState(ViewerVisualState.VALID);
    }

    public void showEmptyViewer() {
        treeContentFlow.getChildren().clear();
        treeContentFlow.setManaged(false);
        treeContentFlow.setVisible(false);
        emptyStateLabel.setManaged(true);
        emptyStateLabel.setVisible(true);
        emptyStateLabel.setText("Drop a JSON anywhere in the window");
        validationStatusLabel.setText("Waiting");
        footerStatusLabel.setText("No JSON loaded");
        viewerContentBox.autosize();
        applyState(ViewerVisualState.EMPTY);
    }

    public void showDraggingState() {
        emptyStateLabel.setText("Release to inspect this JSON file");
        validationStatusLabel.setText("Drop ready");
        footerStatusLabel.setText("Waiting for JSON drop");
        applyState(ViewerVisualState.DRAGGING);
    }

    public void showLoadingState(String fileName) {
        fileNameLabel.setText(fileName);
        fileMetaLabel.setText("Preparing JSON preview");
        validationStatusLabel.setText("Loading");
        footerStatusLabel.setText("Parsing JSON");
        emptyStateLabel.setText("Loading JSON preview...");
        treeContentFlow.getChildren().clear();
        treeContentFlow.setManaged(false);
        treeContentFlow.setVisible(false);
        emptyStateLabel.setManaged(true);
        emptyStateLabel.setVisible(true);
        applyState(ViewerVisualState.LOADING);
    }

    public void showInvalidState(String message) {
        validationStatusLabel.setText("Invalid");
        footerStatusLabel.setText("JSON needs attention");
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
    }

    ViewerVisualState currentState() {
        return currentState;
    }

    private void applyState(ViewerVisualState state) {
        currentState = state;
        viewerShell.getStyleClass().removeAll(
                "viewer-dragging",
                "viewer-loading",
                "viewer-valid",
                "viewer-invalid"
        );
        switch (state) {
            case DRAGGING -> viewerShell.getStyleClass().add("viewer-dragging");
            case LOADING -> viewerShell.getStyleClass().add("viewer-loading");
            case VALID -> viewerShell.getStyleClass().add("viewer-valid");
            case INVALID -> viewerShell.getStyleClass().add("viewer-invalid");
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
        JsonViewerLoadResult result = workflowService.loadFile(jsonPath);
        presentLoadResult(result);
        event.setDropCompleted(true);
        event.consume();
    }

    private java.util.Optional<Path> firstJsonFile(Dragboard dragboard) {
        if (!dragboard.hasFiles()) {
            return java.util.Optional.empty();
        }
        return dragboard.getFiles().stream()
                .map(java.io.File::toPath)
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                .findFirst();
    }

    private void restoreViewFromWorkflow() {
        workflowService.currentView().ifPresentOrElse(this::presentLoadResult, this::showEmptyViewer);
    }

    private void presentLoadResult(JsonViewerLoadResult result) {
        fileNameLabel.setText(result.importResult().fileName());
        fileMetaLabel.setText(formatFileMeta(result.importResult().sizeBytes(), result.historyEntry() != null));

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
            return meta + " • saved to history";
        }
        return meta + " • not persisted";
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
        return validationResult.message() + " (line " + validationResult.line() + ", column " + validationResult.column() + ")";
    }

    @FXML
    void openHistory() {
        uiFlowManager.show(UiScreenId.HISTORY);
    }
}
