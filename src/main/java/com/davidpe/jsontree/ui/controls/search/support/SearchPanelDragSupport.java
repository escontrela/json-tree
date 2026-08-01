package com.davidpe.jsontree.ui.controls.search.support;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import com.davidpe.jsontree.ui.controls.search.model.SearchPanelPosition;

/**
 * Attaches bounded drag behavior to the floating search panel without affecting its inner fields
 * or buttons.
 */
public class SearchPanelDragSupport {

  private final SearchPanelPositioner positioner;

  private Pane hostPane;
  private Region panel;
  private double pointerOffsetX;
  private double pointerOffsetY;
  private boolean positionInitialized;

  public SearchPanelDragSupport(SearchPanelPositioner positioner) {
    this.positioner = positioner;
  }

  public void attach(Pane hostPane, Region panel, Region dragHandle) {
    this.hostPane = hostPane;
    this.panel = panel;

    dragHandle.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleDragPressed);
    dragHandle.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleDragDragged);
    hostPane.widthProperty().addListener((unused, oldValue, newValue) -> clampToViewport());
    hostPane.heightProperty().addListener((unused, oldValue, newValue) -> clampToViewport());
    panel.layoutBoundsProperty().addListener((unused, oldValue, newValue) -> clampToViewport());
  }

  public void resetInitialPosition() {
    positionInitialized = false;
  }

  public void ensureInitialPosition() {
    if (hostPane == null || panel == null || positionInitialized) {
      return;
    }
    if (panel.getWidth() <= 0.0 || panel.getHeight() <= 0.0 || hostPane.getWidth() <= 0.0) {
      Platform.runLater(this::ensureInitialPosition);
      return;
    }
    applyPosition(
        positioner.initialPosition(
            panel.getWidth(), panel.getHeight(), hostPane.getWidth(), hostPane.getHeight()));
    positionInitialized = true;
  }

  public void clampToViewport() {
    if (hostPane == null || panel == null || !positionInitialized) {
      return;
    }
    applyPosition(
        positioner.clamp(
            panel.getLayoutX(),
            panel.getLayoutY(),
            panel.getWidth(),
            panel.getHeight(),
            hostPane.getWidth(),
            hostPane.getHeight()));
  }

  private void handleDragPressed(MouseEvent event) {
    if (hostPane == null || panel == null) {
      return;
    }
    ensureInitialPosition();
    Point2D pointer = hostPane.sceneToLocal(event.getSceneX(), event.getSceneY());
    pointerOffsetX = pointer.getX() - panel.getLayoutX();
    pointerOffsetY = pointer.getY() - panel.getLayoutY();
    event.consume();
  }

  private void handleDragDragged(MouseEvent event) {
    if (hostPane == null || panel == null) {
      return;
    }
    Point2D pointer = hostPane.sceneToLocal(event.getSceneX(), event.getSceneY());
    applyPosition(
        positioner.forDragPointer(
            pointer.getX(),
            pointer.getY(),
            pointerOffsetX,
            pointerOffsetY,
            panel.getWidth(),
            panel.getHeight(),
            hostPane.getWidth(),
            hostPane.getHeight()));
    positionInitialized = true;
    event.consume();
  }

  private void applyPosition(SearchPanelPosition position) {
    panel.relocate(position.x(), position.y());
  }
}
