package com.davidpe.jsontree.ui.window;

import com.davidpe.jsontree.bootstrap.SpringFxmlLoader;
import com.davidpe.jsontree.ui.controller.CurlEditorModalController;
import com.davidpe.jsontree.ui.service.ApplicationThemeService;
import javafx.scene.Parent;
import org.springframework.stereotype.Component;

/**
 * Loads the dedicated curl editor modal FXML and exposes the paired controller.
 */
@Component
public class CurlEditorWindowViewFactory {

  private static final String CURL_EDITOR_FXML = "/com/davidpe/jsontree/ui/curl-editor.fxml";

  private final SpringFxmlLoader springFxmlLoader;
  private final ApplicationThemeService applicationThemeService;

  public CurlEditorWindowViewFactory(
      SpringFxmlLoader springFxmlLoader, ApplicationThemeService applicationThemeService) {
    this.springFxmlLoader = springFxmlLoader;
    this.applicationThemeService = applicationThemeService;
  }

  public CurlEditorWindowView create() {
    Parent root = springFxmlLoader.load(CURL_EDITOR_FXML);
    applicationThemeService.register(root);
    CurlEditorModalController controller =
        (CurlEditorModalController) root.getProperties().get("controller");
    return new CurlEditorWindowView(root, controller);
  }
}
