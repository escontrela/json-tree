package com.davidpe.jsontree.ui.service;

import com.davidpe.jsontree.application.port.in.ViewLargePreviewSettingsUseCase;
import com.davidpe.jsontree.ui.model.ApplicationThemeMode;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import org.springframework.stereotype.Component;

/**
 * Applies and refreshes the shared light/night BMW theme variant across active JavaFX roots.
 */
@Component
public class ApplicationThemeService {

  static final String NIGHT_MODE_STYLE_CLASS = "night-mode";

  private final ViewLargePreviewSettingsUseCase viewLargePreviewSettingsUseCase;
  private final Set<Parent> registeredRoots =
      Collections.newSetFromMap(new WeakHashMap<>());

  public ApplicationThemeService(
      ViewLargePreviewSettingsUseCase viewLargePreviewSettingsUseCase) {
    this.viewLargePreviewSettingsUseCase = viewLargePreviewSettingsUseCase;
  }

  public void register(Parent root) {
    if (root == null) {
      return;
    }
    registeredRoots.add(root);
    applyTheme(root, currentThemeMode());
  }

  public void refreshRegisteredRoots() {
    ApplicationThemeMode themeMode = currentThemeMode();
    registeredRoots.forEach(root -> applyTheme(root, themeMode));
  }

  ApplicationThemeMode currentThemeMode() {
    return ApplicationThemeMode.fromNightModeEnabled(
        viewLargePreviewSettingsUseCase.currentLargePreviewSettings().nightModeEnabled());
  }

  private void applyTheme(Parent root, ApplicationThemeMode themeMode) {
    applyTheme(root.getStyleClass(), themeMode);
  }

  static ObservableList<String> applyTheme(
      ObservableList<String> styleClasses, ApplicationThemeMode themeMode) {
    ObservableList<String> resolvedStyleClasses =
        styleClasses == null ? FXCollections.observableArrayList() : styleClasses;
    resolvedStyleClasses.remove(NIGHT_MODE_STYLE_CLASS);
    if (themeMode.isNightMode()) {
      resolvedStyleClasses.add(NIGHT_MODE_STYLE_CLASS);
    }
    return resolvedStyleClasses;
  }
}
