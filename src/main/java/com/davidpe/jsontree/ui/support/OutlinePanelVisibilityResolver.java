package com.davidpe.jsontree.ui.support;

/**
 * Resolves whether the outline rail should be auto-hidden for the active view.
 *
 * <p>Large-preview documents hide the rail only once per document identity. After that first
 * auto-hide, the resolver preserves whatever visibility the user chose manually.
 */
public class OutlinePanelVisibilityResolver {

  public OutlinePanelVisibilityState resolve(
      boolean currentlyVisible,
      boolean usesLargePreview,
      String currentViewIdentity,
      String autoHiddenIdentity) {
    if (!usesLargePreview || currentViewIdentity == null || currentViewIdentity.isBlank()) {
      return new OutlinePanelVisibilityState(currentlyVisible, null);
    }
    if (currentViewIdentity.equals(autoHiddenIdentity)) {
      return new OutlinePanelVisibilityState(currentlyVisible, autoHiddenIdentity);
    }
    return new OutlinePanelVisibilityState(false, currentViewIdentity);
  }
}
