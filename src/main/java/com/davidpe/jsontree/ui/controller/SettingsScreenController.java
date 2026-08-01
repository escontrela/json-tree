package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.in.SaveLargePreviewSettingsUseCase;
import com.davidpe.jsontree.application.port.in.ViewLargePreviewSettingsUseCase;
import com.davidpe.jsontree.application.service.ProcessMemoryReferenceService;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenController;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.service.ApplicationThemeService;
import com.davidpe.jsontree.ui.support.ApplicationShortcutCatalog;
import com.davidpe.jsontree.ui.support.SettingsFormState;
import com.davidpe.jsontree.ui.support.SettingsFormStateResolver;
import com.davidpe.jsontree.ui.support.SupportedShortcut;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Controller for the non-modal settings screen.
 *
 * <p>The screen edits the runtime snapshot used by future JSON loads while keeping the currently
 * opened document untouched.
 */
@Component
public class SettingsScreenController implements UiScreenController {

  private final UiFlowManager uiFlowManager;
  private final ViewLargePreviewSettingsUseCase viewLargePreviewSettingsUseCase;
  private final SaveLargePreviewSettingsUseCase saveLargePreviewSettingsUseCase;
  private final ProcessMemoryReferenceService processMemoryReferenceService;
  private final SettingsFormStateResolver settingsFormStateResolver;
  private final ApplicationThemeService applicationThemeService;
  private final ApplicationShortcutCatalog applicationShortcutCatalog =
      new ApplicationShortcutCatalog();

  private boolean applyingSnapshot;

  @FXML private BorderPane rootPane;
  @FXML private TextField largePreviewThresholdField;
  @FXML private TextField viewerChunkBytesField;
  @FXML private TextField defaultCurlUserAgentField;
  @FXML private CheckBox prettyLargePreviewCheckBox;
  @FXML private CheckBox nightModeCheckBox;
  @FXML private Label memoryReferenceLabel;
  @FXML private Label memoryWarningLabel;
  @FXML private Label thresholdErrorLabel;
  @FXML private Label chunkErrorLabel;
  @FXML private Label defaultCurlUserAgentErrorLabel;
  @FXML private Button applyButton;
  @FXML private VBox shortcutsListBox;

  public SettingsScreenController(
      ViewLargePreviewSettingsUseCase viewLargePreviewSettingsUseCase,
      SaveLargePreviewSettingsUseCase saveLargePreviewSettingsUseCase,
      ProcessMemoryReferenceService processMemoryReferenceService,
      SettingsFormStateResolver settingsFormStateResolver,
      ApplicationThemeService applicationThemeService,
      @Lazy UiFlowManager uiFlowManager) {
    this.uiFlowManager = uiFlowManager;
    this.viewLargePreviewSettingsUseCase = viewLargePreviewSettingsUseCase;
    this.saveLargePreviewSettingsUseCase = saveLargePreviewSettingsUseCase;
    this.processMemoryReferenceService = processMemoryReferenceService;
    this.settingsFormStateResolver = settingsFormStateResolver;
    this.applicationThemeService = applicationThemeService;
  }

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    renderShortcutRows(applicationShortcutCatalog.supportedShortcuts());
    largePreviewThresholdField
        .textProperty()
        .addListener((unused, oldValue, newValue) -> refreshFormState());
    viewerChunkBytesField
        .textProperty()
        .addListener((unused, oldValue, newValue) -> refreshFormState());
    defaultCurlUserAgentField
        .textProperty()
        .addListener((unused, oldValue, newValue) -> refreshFormState());
    prettyLargePreviewCheckBox
        .selectedProperty()
        .addListener((unused, oldValue, newValue) -> refreshFormState());
    nightModeCheckBox
        .selectedProperty()
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
    if (applyButton.isDisabled()) {
      return;
    }
    saveLargePreviewSettingsUseCase.saveLargePreviewSettings(
        new LargePreviewSettingsSnapshot(
            Long.parseLong(largePreviewThresholdField.getText().trim()),
            Integer.parseInt(viewerChunkBytesField.getText().trim()),
            defaultCurlUserAgentField.getText().trim(),
            prettyLargePreviewCheckBox.isSelected(),
            nightModeCheckBox.isSelected()));
    applicationThemeService.refreshRegisteredRoots();
    loadCurrentSettings();
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
    defaultCurlUserAgentField.setText(state.defaultCurlUserAgentText());
    prettyLargePreviewCheckBox.setSelected(state.prettyLargePreviewSelected());
    nightModeCheckBox.setSelected(state.nightModeSelected());
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
            defaultCurlUserAgentField.getText(),
            prettyLargePreviewCheckBox.isSelected(),
            nightModeCheckBox.isSelected(),
            processMemoryReferenceService.startupMaxMemoryBytes()));
  }

  private void applyFormState(SettingsFormState state) {
    memoryReferenceLabel.setText(state.memoryReferenceText());
    memoryWarningLabel.setText(state.warningText());
    applyWarningStyle(state.warningActive());
    syncErrorLabel(thresholdErrorLabel, state.thresholdErrorText());
    syncErrorLabel(chunkErrorLabel, state.chunkErrorText());
    syncErrorLabel(defaultCurlUserAgentErrorLabel, state.defaultCurlUserAgentErrorText());
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

  private void renderShortcutRows(List<SupportedShortcut> shortcuts) {
    shortcutsListBox.getChildren().clear();
    for (SupportedShortcut shortcut : shortcuts) {
      shortcutsListBox.getChildren().add(createShortcutRow(shortcut));
    }
  }

  private HBox createShortcutRow(SupportedShortcut shortcut) {
    Label titleLabel = new Label(shortcut.title());
    titleLabel.getStyleClass().add("settings-shortcut-title");

    Label descriptionLabel = new Label(shortcut.description());
    descriptionLabel.getStyleClass().add("settings-shortcut-description");
    descriptionLabel.setWrapText(true);

    VBox copyBox = new VBox(4.0, titleLabel, descriptionLabel);
    copyBox.getStyleClass().add("settings-shortcut-copy");

    Label chordLabel = new Label(shortcut.chordLabel());
    chordLabel.getStyleClass().add("settings-shortcut-chord");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox row = new HBox(16.0, copyBox, spacer, chordLabel);
    row.getStyleClass().add("settings-shortcut-row");
    return row;
  }
}
