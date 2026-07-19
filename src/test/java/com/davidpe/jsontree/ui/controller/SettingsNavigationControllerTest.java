package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.in.SaveLargePreviewSettingsUseCase;
import com.davidpe.jsontree.application.port.in.ViewLargePreviewSettingsUseCase;
import com.davidpe.jsontree.application.service.ProcessMemoryReferenceService;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenFactory;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.service.ApplicationThemeService;
import com.davidpe.jsontree.ui.service.TypewriterLabelRevealService;
import com.davidpe.jsontree.ui.service.ZoomWindowCoordinator;
import com.davidpe.jsontree.ui.service.ZoomViewerStateBridge;
import com.davidpe.jsontree.ui.support.SettingsFormStateResolver;
import com.davidpe.jsontree.ui.support.ZoomActionAvailabilityResolver;
import com.davidpe.jsontree.ui.support.ZoomViewerSnapshotFactory;
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
            null,
            null,
            null,
            null,
            new TypewriterLabelRevealService(),
            null,
            new ZoomActionAvailabilityResolver(),
            new NoOpZoomWindowCoordinator(),
            new ZoomViewerStateBridge(),
            new ZoomViewerSnapshotFactory(),
            uiFlowManager);

    controller.openSettings();

    assertEquals(UiScreenId.SETTINGS, uiFlowManager.lastShown());
  }

  @Test
  void settingsBackActionNavigatesToMainScreen() {
    RecordingUiFlowManager uiFlowManager = new RecordingUiFlowManager();
    RecordingSaveUseCase saveUseCase = new RecordingSaveUseCase();
    SettingsScreenController controller =
        new SettingsScreenController(
            new StaticViewUseCase(new LargePreviewSettingsSnapshot(2_048L, 4_096)),
            saveUseCase,
            new ProcessMemoryReferenceService(),
            new SettingsFormStateResolver(),
            new ApplicationThemeService(new StaticViewUseCase(new LargePreviewSettingsSnapshot(2_048L, 4_096))),
            uiFlowManager);

    controller.backToMain();

    assertEquals(UiScreenId.MAIN, uiFlowManager.lastShown());
    assertFalse(saveUseCase.invoked());
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

  private record StaticViewUseCase(LargePreviewSettingsSnapshot snapshot)
      implements ViewLargePreviewSettingsUseCase {

    @Override
    public LargePreviewSettingsSnapshot currentLargePreviewSettings() {
      return snapshot;
    }
  }

  private static final class RecordingSaveUseCase implements SaveLargePreviewSettingsUseCase {

    private boolean invoked;

    @Override
    public LargePreviewSettingsSnapshot saveLargePreviewSettings(
        LargePreviewSettingsSnapshot snapshot) {
      invoked = true;
      return snapshot;
    }

    private boolean invoked() {
      return invoked;
    }
  }

  private static final class NoOpZoomWindowCoordinator implements ZoomWindowCoordinator {

    @Override
    public void openOrFocus() {}
  }
}
