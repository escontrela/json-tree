package com.davidpe.jsontree.ui.service;

import com.davidpe.jsontree.ui.window.ZoomWindowView;
import com.davidpe.jsontree.ui.window.ZoomWindowViewFactory;
import com.davidpe.jsontree.ui.support.ZoomWindowPlacementResolver;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Owns the lifecycle of the dedicated secondary zoom stage.
 */
@Component
public class StageZoomWindowCoordinator implements ZoomWindowCoordinator {

  public static final double INITIAL_WIDTH = 1180.0;
  public static final double INITIAL_HEIGHT = 760.0;

  private final ObjectProvider<Stage> primaryStageProvider;
  private final ZoomWindowViewFactory zoomWindowViewFactory;
  private final ZoomWindowPlacementResolver zoomWindowPlacementResolver;
  private final String applicationTitle;

  private Stage zoomStage;

  public StageZoomWindowCoordinator(
      ObjectProvider<Stage> primaryStageProvider,
      ZoomWindowViewFactory zoomWindowViewFactory,
      ZoomWindowPlacementResolver zoomWindowPlacementResolver,
      String applicationTitle) {
    this.primaryStageProvider = primaryStageProvider;
    this.zoomWindowViewFactory = zoomWindowViewFactory;
    this.zoomWindowPlacementResolver = zoomWindowPlacementResolver;
    this.applicationTitle = applicationTitle;
  }

  @Override
  public void openOrFocus() {
    Stage stage = prepareStage();
    if (stage.isShowing()) {
      stage.toFront();
      stage.requestFocus();
      return;
    }
    centerRelativeToOwner(stage);
    stage.show();
    stage.requestFocus();
  }

  Stage activeStage() {
    return zoomStage;
  }

  Stage prepareStage() {
    if (zoomStage != null) {
      return zoomStage;
    }

    ZoomWindowView view = zoomWindowViewFactory.create();
    Stage stage = new Stage();
    Stage primaryStage = primaryStageProvider.getIfAvailable();
    if (primaryStage != null) {
      stage.initOwner(primaryStage);
    }
    stage.initModality(Modality.NONE);
    stage.setTitle(applicationTitle + " • Zoom");
    stage.setResizable(true);
    stage.setWidth(INITIAL_WIDTH);
    stage.setHeight(INITIAL_HEIGHT);
    stage.setMinWidth(860.0);
    stage.setMinHeight(560.0);
    stage.setScene(new Scene(view.root(), INITIAL_WIDTH, INITIAL_HEIGHT));
    stage.setOnShown(
        unused -> {
          centerRelativeToOwner(stage);
          view.controller().activate();
        });
    stage.setOnHidden(unused -> view.controller().deactivate());
    zoomStage = stage;
    return zoomStage;
  }

  private void centerRelativeToOwner(Stage stage) {
    Stage primaryStage = primaryStageProvider.getIfAvailable();
    if (primaryStage == null || primaryStage.getWidth() <= 0.0 || primaryStage.getHeight() <= 0.0) {
      stage.centerOnScreen();
      return;
    }
    stage.setX(
        zoomWindowPlacementResolver.centeredCoordinate(
            primaryStage.getX(), primaryStage.getWidth(), stage.getWidth()));
    stage.setY(
        zoomWindowPlacementResolver.centeredCoordinate(
            primaryStage.getY(), primaryStage.getHeight(), stage.getHeight()));
  }
}
