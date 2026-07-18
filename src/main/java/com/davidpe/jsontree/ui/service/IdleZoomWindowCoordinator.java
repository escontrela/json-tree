package com.davidpe.jsontree.ui.service;

import org.springframework.stereotype.Component;

/**
 * Placeholder zoom coordinator used until the dedicated zoom window shell is wired.
 */
@Component
public class IdleZoomWindowCoordinator implements ZoomWindowCoordinator {

  @Override
  public void openOrFocus() {
    // TREE-0111 wires the actual secondary window lifecycle.
  }
}
