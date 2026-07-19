package com.davidpe.jsontree.infrastructure.ui;

import com.davidpe.jsontree.application.port.out.ClipboardPort;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JavaFxClipboardAdapter implements ClipboardPort {

  private final JavaFxUiThreadRunner uiThreadRunner;

  @Autowired
  public JavaFxClipboardAdapter(JavaFxUiThreadRunner uiThreadRunner) {
    this.uiThreadRunner = uiThreadRunner;
  }

  @Override
  public void copy(String text) {
    uiThreadRunner.run(
        () -> {
          ClipboardContent content = new ClipboardContent();
          content.putString(text);
          Clipboard.getSystemClipboard().setContent(content);
        });
  }

  @Override
  public Optional<String> readText() {
    return uiThreadRunner.call(
        () -> {
          Clipboard clipboard = Clipboard.getSystemClipboard();
          if (!clipboard.hasString()) {
            return Optional.empty();
          }
          return Optional.ofNullable(clipboard.getString());
        });
  }
}
