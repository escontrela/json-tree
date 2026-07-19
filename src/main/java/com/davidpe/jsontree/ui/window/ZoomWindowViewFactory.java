package com.davidpe.jsontree.ui.window;

import com.davidpe.jsontree.bootstrap.SpringFxmlLoader;
import com.davidpe.jsontree.ui.controller.ZoomWindowController;
import com.davidpe.jsontree.ui.service.ApplicationThemeService;
import javafx.scene.Parent;
import org.springframework.stereotype.Component;

/**
 * Loads the dedicated zoom window FXML and exposes the paired controller.
 */
@Component
public class ZoomWindowViewFactory {

  private static final String ZOOM_FXML = "/com/davidpe/jsontree/ui/zoom.fxml";

  private final SpringFxmlLoader springFxmlLoader;
  private final ApplicationThemeService applicationThemeService;

  public ZoomWindowViewFactory(
      SpringFxmlLoader springFxmlLoader, ApplicationThemeService applicationThemeService) {
    this.springFxmlLoader = springFxmlLoader;
    this.applicationThemeService = applicationThemeService;
  }

  public ZoomWindowView create() {
    Parent root = springFxmlLoader.load(ZOOM_FXML);
    applicationThemeService.register(root);
    ZoomWindowController controller = (ZoomWindowController) root.getProperties().get("controller");
    return new ZoomWindowView(root, controller);
  }
}
