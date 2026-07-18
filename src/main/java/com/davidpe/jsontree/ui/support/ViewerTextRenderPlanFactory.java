package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds render plans for the shared RichTextFX viewer without exposing span details to
 * controllers.
 */
@Component
public class ViewerTextRenderPlanFactory {

  private static final String RAW_JSON_BASE_STYLE_CLASS = "raw-json-text";
  private static final String RAW_JSON_BASE_COLOR = "#2d333a";

  private final AsciiTreeSyntaxHighlighter asciiTreeSyntaxHighlighter;
  private final SearchTextFlowHighlighter searchTextFlowHighlighter;

  public ViewerTextRenderPlanFactory(
      AsciiTreeSyntaxHighlighter asciiTreeSyntaxHighlighter,
      SearchTextFlowHighlighter searchTextFlowHighlighter) {
    this.asciiTreeSyntaxHighlighter = asciiTreeSyntaxHighlighter;
    this.searchTextFlowHighlighter = searchTextFlowHighlighter;
  }

  public TextFlowRenderPlan buildAsciiPlan(
      AsciiTreeDocument document, List<SearchHighlightRange> highlightRanges) {
    return asciiTreeSyntaxHighlighter.buildRenderPlan(document, highlightRanges);
  }

  public TextFlowRenderPlan buildRawPlan(
      String content, List<SearchHighlightRange> highlightRanges) {
    return searchTextFlowHighlighter.buildRenderPlan(
        content, highlightRanges, RAW_JSON_BASE_STYLE_CLASS, RAW_JSON_BASE_COLOR);
  }
}
