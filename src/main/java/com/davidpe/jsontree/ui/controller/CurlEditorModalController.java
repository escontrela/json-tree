package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.CurlDocumentImportResult;
import com.davidpe.jsontree.application.port.in.SubmitCurlCommandUseCase;
import com.davidpe.jsontree.ui.model.CurlEditorSession;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Controller for the reusable curl editor modal window.
 */
@Component
public class CurlEditorModalController {

  private final SubmitCurlCommandUseCase submitCurlCommandUseCase;
  private final UiFlowManager uiFlowManager;

  private CurlEditorSession currentSession = CurlEditorSession.empty(() -> {});
  private long sessionSequence;
  private long submissionSequence;

  @FXML private BorderPane rootPane;
  @FXML private Label modalTitleLabel;
  @FXML private Label modalMetaLabel;
  @FXML private TextArea curlCommandTextArea;
  @FXML private Label curlEditorErrorLabel;
  @FXML private Label curlEditorStateLabel;
  @FXML private Button runCurlButton;
  @FXML private Button cancelButton;

  public CurlEditorModalController(
      SubmitCurlCommandUseCase submitCurlCommandUseCase, UiFlowManager uiFlowManager) {
    this.submitCurlCommandUseCase = submitCurlCommandUseCase;
    this.uiFlowManager = uiFlowManager;
  }

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    applyBusyState(false);
    hideError();
    curlEditorStateLabel.setText("Paste or type one supported curl command.");
  }

  public void prepareSession(CurlEditorSession session) {
    currentSession = session;
    sessionSequence++;
    submissionSequence = 0L;
    modalTitleLabel.setText(session.title());
    modalMetaLabel.setText(session.supportingText());
    curlCommandTextArea.setText(session.initialCommand());
    hideError();
    applyBusyState(false);
    curlEditorStateLabel.setText("Paste or type one supported curl command.");
  }

  public void activate() {
    Platform.runLater(curlCommandTextArea::requestFocus);
  }

  public void deactivate() {
    applyBusyState(false);
    hideError();
  }

  @FXML
  void closeWindow() {
    currentWindow().ifPresent(Window::hide);
  }

  @FXML
  void runCurl() {
    long activeSessionSequence = sessionSequence;
    long requestSequence = ++submissionSequence;
    String command = curlCommandTextArea.getText();
    applyBusyState(true);
    hideError();
    curlEditorStateLabel.setText("Running curl request...");
    CompletableFuture
        .supplyAsync(() -> submitCurlCommandUseCase.submitCurlCommand(command))
        .whenComplete(
            (result, throwable) ->
                Platform.runLater(
                    () ->
                        handleSubmissionResult(
                            activeSessionSequence, requestSequence, result, throwable)));
  }

  private void handleSubmissionResult(
      long activeSessionSequence,
      long requestSequence,
      CurlDocumentImportResult result,
      Throwable throwable) {
    if (activeSessionSequence != sessionSequence || requestSequence != submissionSequence) {
      return;
    }
    applyBusyState(false);
    if (throwable != null || result == null) {
      showError("Unable to run curl right now.");
      return;
    }
    if (!result.successful()) {
      showError(result.message());
      return;
    }
    hideError();
    curlEditorStateLabel.setText("Curl imported successfully.");
    currentSession.onSuccess().run();
    currentWindow().ifPresent(Window::hide);
    uiFlowManager.show(UiScreenId.MAIN);
  }

  private void applyBusyState(boolean busy) {
    runCurlButton.setDisable(busy);
    cancelButton.setDisable(busy);
    curlCommandTextArea.setDisable(busy);
  }

  private void showError(String message) {
    syncErrorLabel(message);
    curlEditorStateLabel.setText("Review the curl command and try again.");
  }

  private void hideError() {
    syncErrorLabel("");
  }

  private void syncErrorLabel(String message) {
    boolean visible = message != null && !message.isBlank();
    curlEditorErrorLabel.setText(visible ? message : "");
    curlEditorErrorLabel.setManaged(visible);
    curlEditorErrorLabel.setVisible(visible);
  }

  private java.util.Optional<Stage> currentWindow() {
    if (rootPane.getScene() == null || rootPane.getScene().getWindow() == null) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(rootPane.getScene().getWindow())
        .filter(Stage.class::isInstance)
        .map(Stage.class::cast);
  }
}
