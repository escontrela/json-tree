package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.ui.controller.SearchPanelController;
import javafx.scene.Parent;

/**
 * Loaded search-panel view paired with its controller.
 */
public record SearchPanelView(Parent root, SearchPanelController controller) {}
