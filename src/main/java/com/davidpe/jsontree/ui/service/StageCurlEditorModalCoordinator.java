package com.davidpe.jsontree.ui.service;

import com.davidpe.jsontree.ui.model.CurlEditorSession;
import com.davidpe.jsontree.ui.support.ZoomWindowPlacementResolver;
import com.davidpe.jsontree.ui.window.CurlEditorWindowView;
import com.davidpe.jsontree.ui.window.CurlEditorWindowViewFactory;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Owns the lifecycle of the reusable curl editor modal window.
 */
@Component
public class StageCurlEditorModalCoordinator implements CurlEditorModalCoordinator {

  static final double INITIAL_WIDTH = 940.0;
  static final double INITIAL_HEIGHT = 620.0;

  private final ObjectProvider<Stage> primaryStageProvider;
  private final CurlEditorWindowViewFactory curlEditorWindowViewFactory;
  private final ZoomWindowPlacementResolver zoomWindowPlacementResolver;
  private final String applicationTitle;

  private Stage modalStage;
  private CurlEditorWindowView modalView;

  public StageCurlEditorModalCoordinator(
      ObjectProvider<Stage> primaryStageProvider,
      CurlEditorWindowViewFactory curlEditorWindowViewFactory,
      ZoomWindowPlacementResolver zoomWindowPlacementResolver,
      String applicationTitle) {
    this.primaryStageProvider = primaryStageProvider;
    this.curlEditorWindowViewFactory = curlEditorWindowViewFactory;
    this.zoomWindowPlacementResolver = zoomWindowPlacementResolver;
    this.applicationTitle = applicationTitle;
  }

  @Override
  public void openNew(Runnable onSuccess) {
    open(CurlEditorSession.empty(onSuccess));
  }

  @Override
  public void openPrefilled(String curlCommand, Runnable onSuccess) {
    open(CurlEditorSession.prefilled(curlCommand, onSuccess));
  }

  private void open(CurlEditorSession session) {
    Stage stage = prepareStage();
    modalView.controller().prepareSession(session);
    centerRelativeToOwner(stage);
    if (stage.isShowing()) {
      stage.toFront();
      stage.requestFocus();
      return;
    }
    stage.show();
    stage.requestFocus();
  }

  Stage activeStage() {
    return modalStage;
  }

  Stage prepareStage() {
    if (modalStage != null) {
      return modalStage;
    }

    modalView = curlEditorWindowViewFactory.create();
    Stage stage = new Stage();
    Stage primaryStage = primaryStageProvider.getIfAvailable();
    if (primaryStage != null) {
      stage.initOwner(primaryStage);
    }
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle(applicationTitle + " • Curl editor");
    stage.setResizable(true);
    stage.setWidth(INITIAL_WIDTH);
    stage.setHeight(INITIAL_HEIGHT);
    stage.setMinWidth(760.0);
    stage.setMinHeight(480.0);
    stage.setScene(new Scene(modalView.root(), INITIAL_WIDTH, INITIAL_HEIGHT));
    stage.setOnShown(unused -> modalView.controller().activate());
    stage.setOnHidden(unused -> modalView.controller().deactivate());
    modalStage = stage;
    return modalStage;
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
