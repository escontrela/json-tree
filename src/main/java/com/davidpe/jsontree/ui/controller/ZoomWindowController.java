package com.davidpe.jsontree.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Controller for the dedicated secondary zoom window shell.
 */
@Component
public class ZoomWindowController {

  @FXML private BorderPane rootPane;

  @FXML private Label zoomModeLabel;

  @FXML private Label zoomTitleLabel;

  @FXML private Label zoomStateLabel;

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    showAwaitingDocument();
  }

  @FXML
  void closeWindow() {
    currentWindow().ifPresent(Window::hide);
  }

  public void showAwaitingDocument() {
    zoomModeLabel.setText("Zoom viewer");
    zoomTitleLabel.setText("Expanded reading surface");
    zoomStateLabel.setText(
        "The current JSON presentation will appear here once the zoom viewer content is wired.");
  }

  private java.util.Optional<Window> currentWindow() {
    if (rootPane.getScene() == null) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.ofNullable(rootPane.getScene().getWindow());
  }
}
