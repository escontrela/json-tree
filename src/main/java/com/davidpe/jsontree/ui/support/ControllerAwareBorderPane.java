package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.ui.screen.UiScreenController;
import javafx.scene.layout.BorderPane;

public class ControllerAwareBorderPane extends BorderPane {

    public void attachController(UiScreenController controller) {
        getProperties().put("controller", controller);
    }
}
