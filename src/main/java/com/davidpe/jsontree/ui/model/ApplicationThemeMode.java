package com.davidpe.jsontree.ui.model;

/**
 * Shared presentation theme mode used across primary and auxiliary JavaFX surfaces.
 */
public enum ApplicationThemeMode {
  LIGHT(false),
  NIGHT(true);

  private final boolean nightMode;

  ApplicationThemeMode(boolean nightMode) {
    this.nightMode = nightMode;
  }

  public static ApplicationThemeMode fromNightModeEnabled(boolean nightModeEnabled) {
    return nightModeEnabled ? NIGHT : LIGHT;
  }

  public boolean isNightMode() {
    return nightMode;
  }
}
