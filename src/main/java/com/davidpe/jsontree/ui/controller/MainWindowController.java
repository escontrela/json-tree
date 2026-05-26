package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.ui.model.ViewerVisualState;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.support.AsciiTreeSyntaxHighlighter;
import com.davidpe.jsontree.ui.support.ControllerAwareBorderPane;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.springframework.stereotype.Component;

@Component
public class MainWindowController implements UiScreenController {

    private final AsciiTreeSyntaxHighlighter syntaxHighlighter;

    public MainWindowController(AsciiTreeSyntaxHighlighter syntaxHighlighter) {
        this.syntaxHighlighter = syntaxHighlighter;
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
        showEmptyViewer();
    }

    public void renderAsciiTree(AsciiTreeDocument document) {
        TextFlow highlightedContent = syntaxHighlighter.highlight(document);
        viewerContentBox.getChildren().set(viewerContentBox.getChildren().indexOf(treeContentFlow), highlightedContent);
        treeContentFlow = highlightedContent;
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
}
