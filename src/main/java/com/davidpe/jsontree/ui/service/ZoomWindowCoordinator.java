package com.davidpe.jsontree.ui.service;

/**
 * Presentation-side coordination contract for the secondary zoom viewer window.
 *
 * <p>The main screen can request the zoom workflow without knowing how the window is built or
 * shown.
 */
public interface ZoomWindowCoordinator {

  void openOrFocus();
}
