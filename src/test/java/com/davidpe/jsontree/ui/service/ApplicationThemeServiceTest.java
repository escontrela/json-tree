package com.davidpe.jsontree.ui.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.in.ViewLargePreviewSettingsUseCase;
import com.davidpe.jsontree.ui.model.ApplicationThemeMode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;

class ApplicationThemeServiceTest {

  @Test
  void currentThemeModeReflectsThePersistedNightModeFlag() {
    MutableViewUseCase viewUseCase =
        new MutableViewUseCase(new LargePreviewSettingsSnapshot(2_048L, 4_096, false, true));
    ApplicationThemeService service = new ApplicationThemeService(viewUseCase);

    assertTrue(service.currentThemeMode().isNightMode());
  }

  @Test
  void applyThemeMutatesStyleClassesWithoutDuplicatingTheNightModeMarker() {
    ObservableList<String> styleClasses = FXCollections.observableArrayList("app-shell");

    ApplicationThemeService.applyTheme(styleClasses, ApplicationThemeMode.NIGHT);
    ApplicationThemeService.applyTheme(styleClasses, ApplicationThemeMode.NIGHT);
    assertTrue(styleClasses.contains(ApplicationThemeService.NIGHT_MODE_STYLE_CLASS));
    assertTrue(styleClasses.stream()
        .filter(ApplicationThemeService.NIGHT_MODE_STYLE_CLASS::equals)
        .count() == 1);

    ApplicationThemeService.applyTheme(styleClasses, ApplicationThemeMode.LIGHT);
    assertFalse(styleClasses.contains(ApplicationThemeService.NIGHT_MODE_STYLE_CLASS));
  }

  private static final class MutableViewUseCase implements ViewLargePreviewSettingsUseCase {

    private LargePreviewSettingsSnapshot snapshot;

    private MutableViewUseCase(LargePreviewSettingsSnapshot snapshot) {
      this.snapshot = snapshot;
    }

    @Override
    public LargePreviewSettingsSnapshot currentLargePreviewSettings() {
      return snapshot;
    }
  }
}
