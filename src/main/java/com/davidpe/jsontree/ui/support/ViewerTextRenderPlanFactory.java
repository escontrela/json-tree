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
  private final SearchTextSpanHighlighter searchTextSpanHighlighter;

  public ViewerTextRenderPlanFactory(
      AsciiTreeSyntaxHighlighter asciiTreeSyntaxHighlighter,
      SearchTextSpanHighlighter searchTextSpanHighlighter) {
    this.asciiTreeSyntaxHighlighter = asciiTreeSyntaxHighlighter;
    this.searchTextSpanHighlighter = searchTextSpanHighlighter;
  }

  public ViewerTextRenderPlan buildAsciiPlan(
      AsciiTreeDocument document, List<SearchHighlightRange> highlightRanges) {
    return asciiTreeSyntaxHighlighter.buildRenderPlan(document, highlightRanges);
  }

  public ViewerTextRenderPlan buildRawPlan(
      String content, List<SearchHighlightRange> highlightRanges) {
    return searchTextSpanHighlighter.buildRenderPlan(
        content, highlightRanges, RAW_JSON_BASE_STYLE_CLASS, RAW_JSON_BASE_COLOR);
  }
}
