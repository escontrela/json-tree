package com.davidpe.jsontree.ui.screen;

import javafx.scene.Scene;
import javafx.stage.Stage;

public interface UiScreen {

    UiScreenId id();

    Stage stage();

    Scene scene();

    UiScreenController controller();

    void show();

    void hide();

    boolean isShowing();
}
