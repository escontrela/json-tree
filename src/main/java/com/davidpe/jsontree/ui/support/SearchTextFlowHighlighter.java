package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.springframework.stereotype.Component;

@Component
public class SearchTextFlowHighlighter {

  private static final String INACTIVE_HIGHLIGHT_COLOR = "#355c8a";
  private static final String ACTIVE_HIGHLIGHT_COLOR = "#1c69d4";
  private final int textNodeBudget;
  private final SearchHighlightRangeNormalizer highlightRangeNormalizer;

  public SearchTextFlowHighlighter() {
    this(new LargePreviewProperties(), new SearchHighlightRangeNormalizer());
  }

  public SearchTextFlowHighlighter(LargePreviewProperties largePreviewProperties) {
    this(largePreviewProperties, new SearchHighlightRangeNormalizer());
  }

  SearchTextFlowHighlighter(
      LargePreviewProperties largePreviewProperties,
      SearchHighlightRangeNormalizer highlightRangeNormalizer) {
    this.textNodeBudget = Math.max(1, largePreviewProperties.getTextNodeBudget());
    this.highlightRangeNormalizer = highlightRangeNormalizer;
  }

  public TextFlowRenderOutcome appendHighlightedText(
      TextFlow textFlow,
      String content,
      List<SearchHighlightRange> highlightRanges,
      String baseStyleClass,
      String baseColorHex
  ) {
    textFlow.getChildren().clear();
    TextFlowRenderPlan renderPlan =
        buildRenderPlan(content, highlightRanges, baseStyleClass, baseColorHex);
    for (TextFlowRenderFragment fragment : renderPlan.fragments()) {
      textFlow.getChildren().add(renderFragment(fragment));
    }
    return renderPlan.guardrailApplied()
        ? TextFlowRenderOutcome.guardrailTriggered()
        : TextFlowRenderOutcome.normalOutcome();
  }

  TextFlowRenderPlan buildRenderPlan(
      String content,
      List<SearchHighlightRange> highlightRanges,
      String baseStyleClass,
      String baseColorHex) {
    if (content == null || content.isEmpty()) {
      return TextFlowRenderPlan.normal(List.of());
    }

    List<SearchHighlightRange> orderedRanges = highlightRangeNormalizer.normalize(highlightRanges);

    List<TextFlowRenderFragment> fragments = new java.util.ArrayList<>();
    int cursor = 0;
    for (SearchHighlightRange range : orderedRanges) {
      if (range.startIndex() > cursor) {
        if (appendFragment(
            fragments,
            buildFragment(content.substring(cursor, range.startIndex()), baseStyleClass, baseColorHex, false, false))) {
          return applyPlainTextFallback(content, baseStyleClass, baseColorHex);
        }
      }
      if (range.endIndex() > range.startIndex()) {
        if (appendFragment(
            fragments,
            buildFragment(
                content.substring(range.startIndex(), range.endIndex()),
                baseStyleClass,
                range.active() ? ACTIVE_HIGHLIGHT_COLOR : INACTIVE_HIGHLIGHT_COLOR,
                true,
                range.active()))) {
          return applyPlainTextFallback(content, baseStyleClass, baseColorHex);
        }
      }
      cursor = Math.max(cursor, range.endIndex());
    }

    if (cursor < content.length()) {
      if (appendFragment(
          fragments,
          buildFragment(content.substring(cursor), baseStyleClass, baseColorHex, false, false))) {
        return applyPlainTextFallback(content, baseStyleClass, baseColorHex);
      }
    }
    return TextFlowRenderPlan.normal(fragments);
  }

  private Text renderFragment(TextFlowRenderFragment fragment) {
    Text node = new Text(fragment.text());
    node.getStyleClass().add(fragment.styleClass());
    node.setFill(Color.web(fragment.colorHex()));
    if (fragment.highlighted()) {
      node.getStyleClass().add("search-match");
      if (fragment.activeHighlight()) {
        node.getStyleClass().add("search-match-active");
        node.setStyle("-fx-font-weight: 700;");
      }
      node.setUnderline(true);
    }
    return node;
  }

  private TextFlowRenderFragment buildFragment(
      String textValue,
      String baseStyleClass,
      String colorHex,
      boolean highlighted,
      boolean active) {
    return new TextFlowRenderFragment(textValue, baseStyleClass, colorHex, highlighted, active);
  }

  private boolean appendFragment(
      List<TextFlowRenderFragment> fragments, TextFlowRenderFragment fragment) {
    if (fragments.size() >= textNodeBudget) {
      return true;
    }
    fragments.add(fragment);
    return false;
  }

  private TextFlowRenderPlan applyPlainTextFallback(
      String content, String baseStyleClass, String baseColorHex) {
    return TextFlowRenderPlan.guardrailFallback(content, baseStyleClass, baseColorHex);
  }
}
