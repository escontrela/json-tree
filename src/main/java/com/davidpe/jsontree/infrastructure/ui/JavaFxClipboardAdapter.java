package com.davidpe.jsontree.infrastructure.ui;

import com.davidpe.jsontree.application.port.out.ClipboardPort;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.stereotype.Component;

@Component
public class JavaFxClipboardAdapter implements ClipboardPort {

  @Override
  public void copy(String text) {
    ClipboardContent content = new ClipboardContent();
    content.putString(text);
    Clipboard.getSystemClipboard().setContent(content);
  }
}
