package com.davidpe.jsontree.ui.window;

import com.davidpe.jsontree.ui.controller.ZoomWindowController;
import javafx.scene.Parent;

/**
 * Loaded zoom window view plus its Spring-managed controller.
 */
public record ZoomWindowView(Parent root, ZoomWindowController controller) {}
