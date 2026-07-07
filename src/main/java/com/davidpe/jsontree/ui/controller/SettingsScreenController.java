package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.in.ViewLargePreviewSettingsUseCase;
import com.davidpe.jsontree.application.service.ProcessMemoryReferenceService;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.support.SettingsFormState;
import com.davidpe.jsontree.ui.support.SettingsFormStateResolver;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
  private final ViewLargePreviewSettingsUseCase viewLargePreviewSettingsUseCase;
  private final ProcessMemoryReferenceService processMemoryReferenceService;
  private final SettingsFormStateResolver settingsFormStateResolver;

  private boolean applyingSnapshot;

  @FXML private BorderPane rootPane;
  @FXML private TextField largePreviewThresholdField;
  @FXML private TextField viewerChunkBytesField;
  @FXML private Label memoryReferenceLabel;
  @FXML private Label memoryWarningLabel;
  @FXML private Label thresholdErrorLabel;
  @FXML private Label chunkErrorLabel;
  @FXML private Button applyButton;

  public SettingsScreenController(
      ViewLargePreviewSettingsUseCase viewLargePreviewSettingsUseCase,
      ProcessMemoryReferenceService processMemoryReferenceService,
      SettingsFormStateResolver settingsFormStateResolver,
      @Lazy UiFlowManager uiFlowManager) {
    this.uiFlowManager = uiFlowManager;
    this.viewLargePreviewSettingsUseCase = viewLargePreviewSettingsUseCase;
    this.processMemoryReferenceService = processMemoryReferenceService;
    this.settingsFormStateResolver = settingsFormStateResolver;
  }

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    largePreviewThresholdField
        .textProperty()
        .addListener((unused, oldValue, newValue) -> refreshFormState());
    viewerChunkBytesField
        .textProperty()
        .addListener((unused, oldValue, newValue) -> refreshFormState());
  }

  @Override
  public void onShow() {
    loadCurrentSettings();
  }

  @FXML
  void backToMain() {
    uiFlowManager.show(UiScreenId.MAIN);
  }

  @FXML
  void applySettings() {
    // Later tickets wire runtime mutation and persistence into this action.
  }

  private void loadCurrentSettings() {
    LargePreviewSettingsSnapshot snapshot =
        viewLargePreviewSettingsUseCase.currentLargePreviewSettings();
    SettingsFormState state =
        settingsFormStateResolver.initialState(
            snapshot, processMemoryReferenceService.startupMaxMemoryBytes());
    applyingSnapshot = true;
    largePreviewThresholdField.setText(state.thresholdText());
    viewerChunkBytesField.setText(state.chunkText());
    applyingSnapshot = false;
    applyFormState(state);
  }

  private void refreshFormState() {
    if (applyingSnapshot) {
      return;
    }
    applyFormState(
        settingsFormStateResolver.resolve(
            largePreviewThresholdField.getText(),
            viewerChunkBytesField.getText(),
            processMemoryReferenceService.startupMaxMemoryBytes()));
  }

  private void applyFormState(SettingsFormState state) {
    memoryReferenceLabel.setText(state.memoryReferenceText());
    memoryWarningLabel.setText(state.warningText());
    applyWarningStyle(state.warningActive());
    syncErrorLabel(thresholdErrorLabel, state.thresholdErrorText());
    syncErrorLabel(chunkErrorLabel, state.chunkErrorText());
    applyButton.setDisable(!state.applyEnabled());
  }

  private void syncErrorLabel(Label label, String text) {
    boolean visible = text != null && !text.isBlank();
    label.setText(visible ? text : "");
    label.setManaged(visible);
    label.setVisible(visible);
  }

  private void applyWarningStyle(boolean warningActive) {
    ObservableList<String> styles = memoryWarningLabel.getStyleClass();
    styles.remove("settings-warning-label-active");
    if (warningActive) {
      styles.add("settings-warning-label-active");
    }
  }
}
