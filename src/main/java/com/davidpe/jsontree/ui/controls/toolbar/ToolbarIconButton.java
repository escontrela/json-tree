package com.davidpe.jsontree.ui.controls.toolbar;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javafx.css.PseudoClass;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Reusable icon-only toolbar button with theme-aware PNG switching.
 *
 * <p>The host screen provides resource-path based light and dark assets plus any ordinary JavaFX
 * button wiring such as action handlers, disablement, tooltip, and accessibility text.
 */
public class ToolbarIconButton extends Button {

  private static final String NIGHT_MODE_STYLE_CLASS = "night-mode";
  private static final double ICON_SIZE = 18.0;
  private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
  private static final PseudoClass SELECTED_PSEUDO_CLASS =
      PseudoClass.getPseudoClass("toolbar-selected");

  private final ImageView iconView = new ImageView();
  private final ToolbarIconAssetResolver assetResolver = new ToolbarIconAssetResolver();
  private final StringProperty lightIconResource =
      new SimpleStringProperty(this, "lightIconResource", "");
  private final StringProperty darkIconResource =
      new SimpleStringProperty(this, "darkIconResource", "");
  private final StringProperty tooltipText = new SimpleStringProperty(this, "tooltipText", "");
  private final BooleanProperty toggleMode = new SimpleBooleanProperty(this, "toggleMode", false);
  private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);
  private final ListChangeListener<String> themeStyleClassListener = change -> refreshIcon();

  private Parent observedThemeRoot;

  public ToolbarIconButton() {
    getStyleClass().add("toolbar-icon-button");
    setMnemonicParsing(false);
    setText(null);
    setGraphic(iconView);
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    setFocusTraversable(true);
    setMinSize(40.0, 40.0);
    setPrefSize(40.0, 40.0);
    setMaxHeight(40.0);

    iconView.setFitWidth(ICON_SIZE);
    iconView.setFitHeight(ICON_SIZE);
    iconView.setPreserveRatio(true);
    iconView.setSmooth(true);
    iconView.setMouseTransparent(true);

    lightIconResource.addListener((unused, oldValue, newValue) -> refreshIcon());
    darkIconResource.addListener((unused, oldValue, newValue) -> refreshIcon());
    tooltipText.addListener((unused, oldValue, newValue) -> refreshTooltip(newValue));
    textProperty()
        .addListener((unused, oldValue, newValue) -> syncTextDrivenAffordances(newValue));
    selected.addListener(
        (unused, oldValue, newValue) ->
            pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, Boolean.TRUE.equals(newValue)));
    sceneProperty()
        .addListener(
            (unused, previousScene, nextScene) ->
                handleSceneChange(previousScene, nextScene));
  }

  public final String getLightIconResource() {
    return lightIconResource.get();
  }

  public final void setLightIconResource(String lightIconResource) {
    this.lightIconResource.set(lightIconResource);
  }

  public final StringProperty lightIconResourceProperty() {
    return lightIconResource;
  }

  public final String getDarkIconResource() {
    return darkIconResource.get();
  }

  public final void setDarkIconResource(String darkIconResource) {
    this.darkIconResource.set(darkIconResource);
  }

  public final StringProperty darkIconResourceProperty() {
    return darkIconResource;
  }

  public final String getTooltipText() {
    return tooltipText.get();
  }

  public final void setTooltipText(String tooltipText) {
    this.tooltipText.set(tooltipText);
  }

  public final StringProperty tooltipTextProperty() {
    return tooltipText;
  }

  public final boolean isToggleMode() {
    return toggleMode.get();
  }

  public final void setToggleMode(boolean toggleMode) {
    this.toggleMode.set(toggleMode);
  }

  public final BooleanProperty toggleModeProperty() {
    return toggleMode;
  }

  public final boolean isSelected() {
    return selected.get();
  }

  public final void setSelected(boolean selected) {
    this.selected.set(selected);
  }

  public final BooleanProperty selectedProperty() {
    return selected;
  }

  @Override
  public void fire() {
    if (isDisabled()) {
      return;
    }
    if (isToggleMode()) {
      setSelected(!isSelected());
    }
    super.fire();
  }

  private void handleSceneChange(Scene previousScene, Scene nextScene) {
    if (previousScene != null) {
      detachThemeRoot(previousScene.getRoot());
    }
    if (nextScene != null) {
      attachThemeRoot(nextScene.getRoot());
      nextScene.rootProperty().addListener((unused, previousRoot, nextRoot) -> {
        detachThemeRoot(previousRoot);
        attachThemeRoot(nextRoot);
      });
    } else {
      observedThemeRoot = null;
    }
    refreshIcon();
  }

  private void attachThemeRoot(Parent themeRoot) {
    if (themeRoot == null || themeRoot == observedThemeRoot) {
      return;
    }
    detachThemeRoot(observedThemeRoot);
    observedThemeRoot = themeRoot;
    observedThemeRoot.getStyleClass().addListener(themeStyleClassListener);
  }

  private void detachThemeRoot(Parent themeRoot) {
    if (themeRoot == null) {
      return;
    }
    themeRoot.getStyleClass().removeListener(themeStyleClassListener);
    if (themeRoot == observedThemeRoot) {
      observedThemeRoot = null;
    }
  }

  private void refreshTooltip(String nextText) {
    if (nextText == null || nextText.isBlank()) {
      setTooltip(null);
      return;
    }
    Tooltip currentTooltip = getTooltip();
    if (currentTooltip == null) {
      setTooltip(new Tooltip(nextText));
      return;
    }
    currentTooltip.setText(nextText);
  }

  private void refreshIcon() {
    String resourcePath = resolveThemeResourcePath();
    if (resourcePath == null || resourcePath.isBlank()) {
      iconView.setImage(null);
      return;
    }
    iconView.setImage(resolveImage(resourcePath));
  }

  private void syncTextDrivenAffordances(String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    setAccessibleText(text);
    if (tooltipText.get() == null || tooltipText.get().isBlank()) {
      refreshTooltip(text);
    }
  }

  private String resolveThemeResourcePath() {
    return assetResolver.resolve(
        lightIconResource.get(),
        darkIconResource.get(),
        isNightModeActive());
  }

  private boolean isNightModeActive() {
    return observedThemeRoot != null
        && observedThemeRoot.getStyleClass().contains(NIGHT_MODE_STYLE_CLASS);
  }

  private Image resolveImage(String resourcePath) {
    return IMAGE_CACHE.computeIfAbsent(
        resourcePath,
        key -> {
          URL resource = ToolbarIconButton.class.getResource(key);
          if (resource == null) {
            throw new IllegalArgumentException("Missing toolbar icon resource: " + key);
          }
          return new Image(resource.toExternalForm(), true);
        });
  }
}
