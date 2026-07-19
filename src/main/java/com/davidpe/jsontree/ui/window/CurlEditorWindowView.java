package com.davidpe.jsontree.ui.window;

import com.davidpe.jsontree.ui.controller.CurlEditorModalController;
import javafx.scene.Parent;

/**
 * Loaded curl editor modal view paired with its Spring-managed controller.
 */
public record CurlEditorWindowView(Parent root, CurlEditorModalController controller) {}
