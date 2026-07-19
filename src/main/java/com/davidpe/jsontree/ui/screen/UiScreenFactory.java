package com.davidpe.jsontree.ui.screen;

import com.davidpe.jsontree.bootstrap.SpringFxmlLoader;
import com.davidpe.jsontree.ui.service.ApplicationThemeService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UiScreenFactory {

  private static final double INITIAL_X = 87.0;
  private static final double INITIAL_Y = 42.0;
  private static final double MIN_WIDTH = 1294.0;
  private static final double MIN_HEIGHT = 798.0;

  private final Stage primaryStage;
  private final SpringFxmlLoader springFxmlLoader;
  private final ApplicationThemeService applicationThemeService;

  public UiScreenFactory(
      Stage primaryStage,
      SpringFxmlLoader springFxmlLoader,
      ApplicationThemeService applicationThemeService) {
    this.primaryStage = primaryStage;
    this.springFxmlLoader = springFxmlLoader;
    this.applicationThemeService = applicationThemeService;
  }

  public UiScreen create(UiScreenId uiScreenId) {
    Parent root = springFxmlLoader.load(uiScreenId.fxmlPath());
    applicationThemeService.register(root);
    UiScreenController controller = (UiScreenController) root.getProperties().get("controller");

    Scene scene = primaryStage.getScene();
    if (scene == null) {
      scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
      primaryStage.setX(INITIAL_X);
      primaryStage.setY(INITIAL_Y);
      primaryStage.setWidth(MIN_WIDTH);
      primaryStage.setHeight(MIN_HEIGHT);
      primaryStage.setScene(scene);
    } else {
      scene.setRoot(root);
    }

    String stylesheet =
        UiScreenFactory.class.getResource("/com/davidpe/jsontree/ui/styles.css").toExternalForm();
    if (!scene.getStylesheets().contains(stylesheet)) {
      scene.getStylesheets().add(stylesheet);
    }

    primaryStage.setMinWidth(MIN_WIDTH);
    primaryStage.setMinHeight(MIN_HEIGHT);

    return new StageUiScreen(uiScreenId, primaryStage, scene, controller);
  }
}
