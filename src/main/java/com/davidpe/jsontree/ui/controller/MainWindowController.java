package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.support.ControllerAwareBorderPane;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.springframework.stereotype.Component;

@Component
public class MainWindowController implements UiScreenController {

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
    private VBox viewerContentBox;

    @FXML
    private Text treeContentText;

    @FXML
    public void initialize() {
        rootPane.attachController(this);
        fileNameLabel.setText("No file loaded");
        fileMetaLabel.setText("Drop a JSON anywhere in the window");
        validationStatusLabel.setText("Waiting");
        emptyStateLabel.setText("Drop a JSON anywhere in the window");
        footerStatusLabel.setText("No JSON loaded");
        showEmptyViewer();
    }

    public void renderAsciiTree(AsciiTreeDocument document) {
        treeContentText.setText(document.content());
        treeContentText.setManaged(true);
        treeContentText.setVisible(true);
        emptyStateLabel.setManaged(false);
        emptyStateLabel.setVisible(false);
        footerStatusLabel.setText("Rendered " + document.lineCount() + " lines");
        viewerScrollPane.setHvalue(0);
        viewerScrollPane.setVvalue(0);
    }

    public void showEmptyViewer() {
        treeContentText.setText("");
        treeContentText.setManaged(false);
        treeContentText.setVisible(false);
        emptyStateLabel.setManaged(true);
        emptyStateLabel.setVisible(true);
        viewerContentBox.autosize();
    }
}
