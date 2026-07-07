package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenFactory;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import org.junit.jupiter.api.Test;

class SettingsNavigationControllerTest {

  @Test
  void mainWindowSettingsActionNavigatesToSettingsScreen() {
    RecordingUiFlowManager uiFlowManager = new RecordingUiFlowManager();
    MainWindowController controller =
        new MainWindowController(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            uiFlowManager);

    controller.openSettings();

    assertEquals(UiScreenId.SETTINGS, uiFlowManager.lastShown());
  }

  @Test
  void settingsBackActionNavigatesToMainScreen() {
    RecordingUiFlowManager uiFlowManager = new RecordingUiFlowManager();
    SettingsScreenController controller =
        new SettingsScreenController(null, null, null, uiFlowManager);

    controller.backToMain();

    assertEquals(UiScreenId.MAIN, uiFlowManager.lastShown());
  }

  private static final class RecordingUiFlowManager extends UiFlowManager {

    private UiScreenId lastShown;

    private RecordingUiFlowManager() {
      super((UiScreenFactory) null);
    }

    @Override
    public void show(UiScreenId uiScreenId) {
      lastShown = uiScreenId;
    }

    private UiScreenId lastShown() {
      return lastShown;
    }
  }
}
