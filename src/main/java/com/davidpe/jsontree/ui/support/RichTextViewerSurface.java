package com.davidpe.jsontree.ui.support;

import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

/**
 * Read-only RichTextFX viewer wrapper used by the JavaFX layer.
 *
 * <p>This wrapper hides the concrete RichTextFX setup from controllers so later tickets can evolve
 * the rendering pipeline without leaking {@link CodeArea} wiring into screen logic.
 */
public final class RichTextViewerSurface {

  private final CodeArea codeArea;
  private final StackPane container;

  RichTextViewerSurface() {
    this.codeArea = new CodeArea();
    this.codeArea.setEditable(false);
    this.codeArea.setWrapText(false);
    this.codeArea.setFocusTraversable(true);
    this.codeArea.getStyleClass().add("rich-text-viewer");

    VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
    scrollPane.getStyleClass().add("rich-text-viewer-scroll");

    this.container = new StackPane(scrollPane);
    this.container.getStyleClass().add("rich-text-viewer-shell");
    this.container.setManaged(false);
    this.container.setVisible(false);
  }

  public StackPane view() {
    return container;
  }

  public void show() {
    container.setManaged(true);
    container.setVisible(true);
  }

  public void hide() {
    container.setManaged(false);
    container.setVisible(false);
  }

  public void clear() {
    codeArea.clear();
  }

  public void replaceText(String text) {
    codeArea.replaceText(text == null ? "" : text);
  }

  public String text() {
    return codeArea.getText();
  }

  public boolean editable() {
    return codeArea.isEditable();
  }
}
