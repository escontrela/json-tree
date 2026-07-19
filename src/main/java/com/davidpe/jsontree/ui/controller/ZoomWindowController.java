package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import com.davidpe.jsontree.ui.service.ZoomViewerStateBridge;
import com.davidpe.jsontree.ui.support.RichTextViewerFactory;
import com.davidpe.jsontree.ui.support.RichTextViewerSurface;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Controller for the dedicated secondary zoom window shell.
 */
@Component
public class ZoomWindowController {

  private final RichTextViewerFactory richTextViewerFactory;
  private final ZoomViewerStateBridge zoomViewerStateBridge;

  private RichTextViewerSurface richTextViewerSurface;
  private Runnable zoomSubscriptionRelease;

  @FXML private BorderPane rootPane;

  @FXML private Label zoomModeLabel;

  @FXML private Label zoomTitleLabel;

  @FXML private Label zoomMetaLabel;

  @FXML private StackPane zoomViewerHost;

  @FXML private Label zoomStateLabel;

  public ZoomWindowController(
      RichTextViewerFactory richTextViewerFactory, ZoomViewerStateBridge zoomViewerStateBridge) {
    this.richTextViewerFactory = richTextViewerFactory;
    this.zoomViewerStateBridge = zoomViewerStateBridge;
  }

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    richTextViewerSurface = richTextViewerFactory.create();
    zoomViewerHost.getChildren().setAll(richTextViewerSurface.view());
    showAwaitingDocument();
  }

  @FXML
  void closeWindow() {
    currentWindow().ifPresent(Window::hide);
  }

  public void showAwaitingDocument() {
    presentSnapshot(
        ZoomViewerSnapshot.empty(
            "JSON -> TREE • Zoom",
            "Zoom viewer",
            "Expanded reading surface",
            "Open a JSON in the main workspace to populate this reading surface."));
  }

  public void activate() {
    if (zoomSubscriptionRelease != null) {
      return;
    }
    zoomSubscriptionRelease = zoomViewerStateBridge.subscribe(this::presentSnapshot);
  }

  public void deactivate() {
    if (zoomSubscriptionRelease == null) {
      return;
    }
    zoomSubscriptionRelease.run();
    zoomSubscriptionRelease = null;
  }

  String viewerText() {
    return richTextViewerSurface.text();
  }

  String currentModeLabel() {
    return zoomModeLabel.getText();
  }

  boolean showingDocument() {
    return zoomViewerHost.isVisible();
  }

  boolean viewerEditable() {
    return richTextViewerSurface.editable();
  }

  private void presentSnapshot(ZoomViewerSnapshot snapshot) {
    if (snapshot == null) {
      showAwaitingDocument();
      return;
    }
    zoomModeLabel.setText(snapshot.modeLabel());
    zoomTitleLabel.setText(snapshot.documentTitle());
    applyMeta(snapshot.documentMeta());
    updateWindowTitle(snapshot.windowTitle());

    if (snapshot.renderable() && snapshot.renderPlan() != null) {
      zoomViewerHost.setManaged(true);
      zoomViewerHost.setVisible(true);
      richTextViewerSurface.showStyledText(
          snapshot.renderPlan().fragments(), snapshot.contentStyleClass());
      richTextViewerSurface.scrollToTop();
      zoomStateLabel.setManaged(false);
      zoomStateLabel.setVisible(false);
      return;
    }

    richTextViewerSurface.clear();
    richTextViewerSurface.hide();
    zoomViewerHost.setManaged(false);
    zoomViewerHost.setVisible(false);
    zoomStateLabel.setText(snapshot.emptyStateMessage());
    zoomStateLabel.setManaged(true);
    zoomStateLabel.setVisible(true);
  }

  private void applyMeta(String documentMeta) {
    boolean visible = documentMeta != null && !documentMeta.isBlank();
    zoomMetaLabel.setText(visible ? documentMeta : "");
    zoomMetaLabel.setManaged(visible);
    zoomMetaLabel.setVisible(visible);
  }

  private void updateWindowTitle(String windowTitle) {
    currentWindow()
        .filter(Stage.class::isInstance)
        .map(Stage.class::cast)
        .ifPresent(stage -> stage.setTitle(windowTitle));
  }

  private java.util.Optional<Window> currentWindow() {
    if (rootPane.getScene() == null) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.ofNullable(rootPane.getScene().getWindow());
  }
}
