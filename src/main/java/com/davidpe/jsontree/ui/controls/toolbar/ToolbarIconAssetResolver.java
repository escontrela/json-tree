package com.davidpe.jsontree.ui.controls.toolbar;

/**
 * Resolves the active icon resource path for themed toolbar icon controls.
 */
public class ToolbarIconAssetResolver {

  public String resolve(String lightResourcePath, String darkResourcePath, boolean nightMode) {
    if (nightMode && darkResourcePath != null && !darkResourcePath.isBlank()) {
      return darkResourcePath;
    }
    return lightResourcePath == null ? "" : lightResourcePath;
  }
}
