package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.bootstrap.SpringFxmlLoader;
import com.davidpe.jsontree.ui.controller.SearchPanelController;
import org.springframework.stereotype.Component;

/**
 * Loads the reusable floating search panel FXML and exposes the paired controller.
 */
@Component
public class SearchPanelViewFactory {

  private static final String SEARCH_PANEL_FXML = "/com/davidpe/jsontree/ui/search-panel.fxml";

  private final SpringFxmlLoader springFxmlLoader;

  public SearchPanelViewFactory(SpringFxmlLoader springFxmlLoader) {
    this.springFxmlLoader = springFxmlLoader;
  }

  public SearchPanelView create() {
    javafx.scene.Parent root = springFxmlLoader.load(SEARCH_PANEL_FXML);
    SearchPanelController controller =
        (SearchPanelController) root.getProperties().get("controller");
    return new SearchPanelView(root, controller);
  }
}
