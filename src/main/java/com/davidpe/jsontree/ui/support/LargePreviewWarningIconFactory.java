package com.davidpe.jsontree.ui.support;

import java.util.Objects;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class LargePreviewWarningIconFactory {

  private static final String WARNING_ICON_RESOURCE =
      "/com/davidpe/jsontree/ui/images/warning-35dp.png";
  private static final Image WARNING_ICON =
      new Image(
          Objects.requireNonNull(
                  LargePreviewWarningIconFactory.class.getResourceAsStream(WARNING_ICON_RESOURCE),
                  "Missing warning icon resource: " + WARNING_ICON_RESOURCE));

  private LargePreviewWarningIconFactory() {}

  public static ImageView create(double size) {
    ImageView imageView = new ImageView(WARNING_ICON);
    imageView.setFitWidth(size);
    imageView.setFitHeight(size);
    imageView.setPreserveRatio(true);
    imageView.setMouseTransparent(true);
    imageView.getStyleClass().add("warning-icon");
    return imageView;
  }
}
