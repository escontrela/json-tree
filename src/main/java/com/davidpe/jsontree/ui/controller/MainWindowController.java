package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.support.ControllerAwareBorderPane;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
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
    public void initialize() {
        rootPane.attachController(this);
        fileNameLabel.setText("No file loaded");
        fileMetaLabel.setText("Drop a JSON anywhere in the window");
        validationStatusLabel.setText("Waiting");
        emptyStateLabel.setText("Drop a JSON anywhere in the window");
        footerStatusLabel.setText("No JSON loaded");
    }
}
