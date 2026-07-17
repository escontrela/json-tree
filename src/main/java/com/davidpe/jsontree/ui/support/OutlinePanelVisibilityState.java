package com.davidpe.jsontree.ui.support;

/**
 * UI-only state for the right outline rail visibility.
 *
 * <p>The state keeps both the visible flag and the identity of the large-preview document that has
 * already been auto-hidden, so the controller can preserve a user's manual reopen within the same
 * session.
 */
public record OutlinePanelVisibilityState(boolean visible, String autoHiddenIdentity) {}
