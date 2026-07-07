package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Shell controller for the non-modal settings screen.
 *
 * <p>The initial ticket only wires navigation and layout shell actions. Runtime settings editing is
 * introduced in follow-up tickets.
 */
@Component
public class SettingsScreenController implements UiScreenController {

  private final UiFlowManager uiFlowManager;

  @FXML private BorderPane rootPane;

  public SettingsScreenController(@Lazy UiFlowManager uiFlowManager) {
    this.uiFlowManager = uiFlowManager;
  }

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
  }

  @FXML
  void backToMain() {
    uiFlowManager.show(UiScreenId.MAIN);
  }

  @FXML
  void applySettings() {
    // Later tickets wire runtime mutation and persistence into this action.
  }
}
