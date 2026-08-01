package com.davidpe.jsontree.ui.controls.search.model;

import com.davidpe.jsontree.ui.controls.search.controller.SearchPanelController;
import javafx.scene.Parent;

/**
 * Loaded search-panel view paired with its controller.
 */
public record SearchPanelView(Parent root, SearchPanelController controller) {}
