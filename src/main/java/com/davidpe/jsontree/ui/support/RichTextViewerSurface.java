package com.davidpe.jsontree.ui.support;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * Read-only RichTextFX viewer wrapper used by the JavaFX layer.
 *
 * <p>This wrapper hides the concrete RichTextFX setup from controllers so later tickets can evolve
 * the rendering pipeline without leaking {@link CodeArea} wiring into screen logic.
 */
public final class RichTextViewerSurface {

  private static final List<String> CONTENT_STYLE_CLASSES =
      List.of("tree-content", "raw-json-content");

  private final CodeArea codeArea;
  private final StackPane container;
  private final RichTextViewportScrollResolver viewportScrollResolver;

  RichTextViewerSurface() {
    this.viewportScrollResolver = new RichTextViewportScrollResolver();
    this.codeArea = new CodeArea();
    this.codeArea.setEditable(false);
    this.codeArea.setWrapText(false);
    this.codeArea.setFocusTraversable(true);
    this.codeArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    this.codeArea.getStyleClass().add("rich-text-viewer");

    VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
    scrollPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    scrollPane.getStyleClass().add("rich-text-viewer-scroll");

    this.container = new StackPane(scrollPane);
    this.container.setAlignment(Pos.TOP_LEFT);
    this.container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
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

  public void showText(String text, String contentStyleClass) {
    replaceText(text);
    applyPlainStyleSpans();
    applyContentStyleClass(contentStyleClass);
    show();
  }

  public void showStyledText(
      List<ViewerTextRenderFragment> fragments, String contentStyleClass) {
    StringBuilder contentBuilder = new StringBuilder();
    StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
    for (ViewerTextRenderFragment fragment : fragments) {
      String fragmentText = fragment.text() == null ? "" : fragment.text();
      if (fragmentText.isEmpty()) {
        continue;
      }
      contentBuilder.append(fragmentText);
      spansBuilder.add(resolveStyleClasses(fragment), fragmentText.length());
    }

    replaceText(contentBuilder.toString());
    if (codeArea.getLength() > 0) {
      codeArea.setStyleSpans(0, spansBuilder.create());
    }
    applyContentStyleClass(contentStyleClass);
    show();
  }

  public boolean hasContentStyleClass(String styleClass) {
    return codeArea.getStyleClass().contains(styleClass);
  }

  public List<String> styleClassesAt(int offset) {
    if (codeArea.getLength() == 0) {
      return List.of();
    }
    int clampedOffset = Math.max(0, Math.min(offset, codeArea.getLength() - 1));
    return List.copyOf(codeArea.getStyleOfChar(clampedOffset));
  }

  public String text() {
    return codeArea.getText();
  }

  public boolean editable() {
    return codeArea.isEditable();
  }

  public void scrollToTop() {
    codeArea.moveTo(0);
    codeArea.requestFollowCaret();
  }

  public void scrollToOffset(int offset) {
    int clampedOffset = Math.max(0, Math.min(offset, codeArea.getLength()));
    codeArea.moveTo(clampedOffset);
    codeArea.requestFollowCaret();
  }

  public double verticalScrollValue() {
    Double estimatedScrollY = codeArea.estimatedScrollYProperty().getValue();
    return viewportScrollResolver.scrollValue(
        estimatedScrollY == null ? 0.0 : estimatedScrollY,
        viewportHeight(),
        totalContentHeightEstimate());
  }

  public double viewportHeight() {
    return Math.max(0.0, codeArea.getViewportHeight());
  }

  public double totalContentHeightEstimate() {
    Double estimate = codeArea.totalHeightEstimateProperty().getValue();
    return estimate == null ? 0.0 : Math.max(0.0, estimate);
  }

  public void scrollToVerticalValue(double scrollValue) {
    codeArea.scrollYToPixel(
        viewportScrollResolver.scrollOffset(scrollValue, viewportHeight(), totalContentHeightEstimate()));
  }

  public int firstVisibleParagraphIndex() {
    if (codeArea.getParagraphs().isEmpty()) {
      return 0;
    }
    try {
      return Math.max(0, codeArea.visibleParToAllParIndex(0));
    } catch (RuntimeException exception) {
      return 0;
    }
  }

  public void addViewportChangeListener(Runnable listener) {
    Objects.requireNonNull(listener, "listener must not be null");
    ChangeListener<Number> metricsListener = (unused, oldValue, newValue) -> listener.run();
    codeArea.estimatedScrollYProperty().addListener(metricsListener);
    codeArea.totalHeightEstimateProperty().addListener(metricsListener);
    codeArea.heightProperty().addListener(metricsListener);
  }

  private void applyContentStyleClass(String contentStyleClass) {
    codeArea.getStyleClass().removeAll(CONTENT_STYLE_CLASSES);
    if (contentStyleClass != null && !contentStyleClass.isBlank()) {
      codeArea.getStyleClass().add(contentStyleClass);
    }
  }

  private void applyPlainStyleSpans() {
    if (codeArea.getLength() == 0) {
      return;
    }
    StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
    spansBuilder.add(Collections.emptyList(), codeArea.getLength());
    codeArea.setStyleSpans(0, spansBuilder.create());
  }

  private Collection<String> resolveStyleClasses(ViewerTextRenderFragment fragment) {
    java.util.ArrayList<String> styleClasses = new java.util.ArrayList<>();
    if (fragment.styleClass() != null && !fragment.styleClass().isBlank()) {
      styleClasses.add(fragment.styleClass());
    }
    if (fragment.highlighted()) {
      styleClasses.add("search-match");
      if (fragment.activeHighlight()) {
        styleClasses.add("search-match-active");
      }
    }
    return List.copyOf(styleClasses);
  }
}
