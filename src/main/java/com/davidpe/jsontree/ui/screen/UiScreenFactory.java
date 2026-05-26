package com.davidpe.jsontree.ui.screen;

import com.davidpe.jsontree.bootstrap.SpringFxmlLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UiScreenFactory {

    private final Stage primaryStage;
    private final SpringFxmlLoader springFxmlLoader;

    public UiScreenFactory(Stage primaryStage, SpringFxmlLoader springFxmlLoader) {
        this.primaryStage = primaryStage;
        this.springFxmlLoader = springFxmlLoader;
    }

    public UiScreen create(UiScreenId uiScreenId) {
        Parent root = springFxmlLoader.load(uiScreenId.fxmlPath());
        UiScreenController controller = (UiScreenController) root.getProperties().get("controller");

        Scene scene = new Scene(root, uiScreenId.width(), uiScreenId.height());
        scene.getStylesheets().add(UiScreenFactory.class.getResource("/com/davidpe/jsontree/ui/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(860);
        primaryStage.setMinHeight(580);

        return new StageUiScreen(uiScreenId, primaryStage, scene, controller);
    }
}
