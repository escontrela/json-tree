package com.davidpe.jsontree.ui.controls.search.controller;

import com.davidpe.jsontree.ui.controls.toolbar.ToolbarIconButton;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelCropState;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelMessageTone;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelViewState;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller for a single floating search panel instance.
 *
 * <p>The panel can be loaded independently into the main workspace and the zoom window, so the
 * controller must be prototype-scoped to avoid cross-window state leakage between those surfaces.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SearchPanelController {

  @FXML private VBox searchPanelRoot;
  @FXML private HBox searchPanelDragHandle;
  @FXML private Label searchPanelOccurrenceLabel;
  @FXML private TextField searchPanelQueryField;
  @FXML private Label searchPanelHelperLabel;
  @FXML private ToolbarIconButton searchPanelSubmitButton;
  @FXML private ToolbarIconButton searchPanelPreviousButton;
  @FXML private ToolbarIconButton searchPanelNextButton;
  @FXML private ToolbarIconButton searchPanelClearButton;
  @FXML private ToolbarIconButton searchPanelCropButton;

  private Consumer<String> submitHandler = unused -> {};
  private Runnable previousHandler = () -> {};
  private Runnable nextHandler = () -> {};
  private Runnable clearHandler = () -> {};
  private Runnable cropHandler = () -> {};
  private Runnable closeHandler = () -> {};

  @FXML
  public void initialize() {
    searchPanelRoot.getProperties().put("controller", this);
    searchPanelRoot.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() == KeyCode.ESCAPE) {
            closeHandler.run();
            event.consume();
          }
        });
    applyState(SearchPanelViewState.hidden());
  }

  public void bindHandlers(
      Consumer<String> submitHandler,
      Runnable previousHandler,
      Runnable nextHandler,
      Runnable clearHandler,
      Runnable cropHandler,
      Runnable closeHandler) {
    this.submitHandler = submitHandler == null ? unused -> {} : submitHandler;
    this.previousHandler = previousHandler == null ? () -> {} : previousHandler;
    this.nextHandler = nextHandler == null ? () -> {} : nextHandler;
    this.clearHandler = clearHandler == null ? () -> {} : clearHandler;
    this.cropHandler = cropHandler == null ? () -> {} : cropHandler;
    this.closeHandler = closeHandler == null ? () -> {} : closeHandler;
  }

  public void applyState(SearchPanelViewState state) {
    SearchPanelViewState resolvedState = state == null ? SearchPanelViewState.hidden() : state;
    searchPanelRoot.setManaged(resolvedState.visible());
    searchPanelRoot.setVisible(resolvedState.visible());
    if (!Objects.equals(searchPanelQueryField.getText(), resolvedState.queryText())) {
      searchPanelQueryField.setText(resolvedState.queryText());
    }
    searchPanelOccurrenceLabel.setText(resolvedState.occurrenceText());
    searchPanelHelperLabel.setText(resolvedState.helperText());
    searchPanelSubmitButton.setDisable(!resolvedState.submitEnabled());
    searchPanelPreviousButton.setDisable(!resolvedState.previousEnabled());
    searchPanelNextButton.setDisable(!resolvedState.nextEnabled());
    searchPanelClearButton.setDisable(!resolvedState.clearEnabled());
    applyCropState(resolvedState.cropState());
    searchPanelHelperLabel
        .getStyleClass()
        .removeAll(
            "search-panel-helper-muted",
            "search-panel-helper-accent",
            "search-panel-helper-error");
    searchPanelHelperLabel.getStyleClass().add(helperStyleClass(resolvedState.helperTone()));
  }

  public void revealAndFocus() {
    searchPanelRoot.setManaged(true);
    searchPanelRoot.setVisible(true);
    Platform.runLater(
        () -> {
          searchPanelQueryField.requestFocus();
          searchPanelQueryField.selectAll();
        });
  }

  public VBox root() {
    return searchPanelRoot;
  }

  public HBox dragHandle() {
    return searchPanelDragHandle;
  }

  public String queryText() {
    return searchPanelQueryField.getText();
  }

  public boolean isShowing() {
    return searchPanelRoot.isVisible();
  }

  public boolean isEditingQuery() {
    return searchPanelQueryField.isFocused();
  }

  public void hidePanel() {
    searchPanelRoot.setManaged(false);
    searchPanelRoot.setVisible(false);
  }

  @FXML
  void submitSearch() {
    submitHandler.accept(queryText());
  }

  @FXML
  void showPreviousResult() {
    previousHandler.run();
  }

  @FXML
  void showNextResult() {
    nextHandler.run();
  }

  @FXML
  void clearSearch() {
    clearHandler.run();
  }

  @FXML
  void toggleCrop() {
    cropHandler.run();
  }

  @FXML
  void closePanel() {
    closeHandler.run();
  }

  private String helperStyleClass(SearchPanelMessageTone tone) {
    if (tone == SearchPanelMessageTone.ERROR) {
      return "search-panel-helper-error";
    }
    if (tone == SearchPanelMessageTone.ACCENT) {
      return "search-panel-helper-accent";
    }
    return "search-panel-helper-muted";
  }

  private void applyCropState(SearchPanelCropState cropState) {
    SearchPanelCropState resolvedState =
        cropState == null ? SearchPanelCropState.hidden() : cropState;
    searchPanelCropButton.setManaged(resolvedState.visible());
    searchPanelCropButton.setVisible(resolvedState.visible());
    searchPanelCropButton.setDisable(!resolvedState.enabled());
    searchPanelCropButton.setSelected(resolvedState.selected());
    searchPanelCropButton.setTooltipText(resolvedState.tooltipText());
    searchPanelCropButton.setAccessibleText(resolvedState.accessibleText());
  }
}
