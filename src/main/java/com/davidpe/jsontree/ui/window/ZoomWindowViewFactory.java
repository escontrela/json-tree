package com.davidpe.jsontree.ui.window;

import com.davidpe.jsontree.bootstrap.SpringFxmlLoader;
import com.davidpe.jsontree.ui.controller.ZoomWindowController;
import javafx.scene.Parent;
import org.springframework.stereotype.Component;

/**
 * Loads the dedicated zoom window FXML and exposes the paired controller.
 */
@Component
public class ZoomWindowViewFactory {

  private static final String ZOOM_FXML = "/com/davidpe/jsontree/ui/zoom.fxml";

  private final SpringFxmlLoader springFxmlLoader;

  public ZoomWindowViewFactory(SpringFxmlLoader springFxmlLoader) {
    this.springFxmlLoader = springFxmlLoader;
  }

  public ZoomWindowView create() {
    Parent root = springFxmlLoader.load(ZOOM_FXML);
    ZoomWindowController controller = (ZoomWindowController) root.getProperties().get("controller");
    return new ZoomWindowView(root, controller);
  }
}
