package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SearchTextSpanHighlighter {

  private final SearchHighlightRangeNormalizer highlightRangeNormalizer;

  public SearchTextSpanHighlighter() {
    this(new LargePreviewProperties(), new SearchHighlightRangeNormalizer());
  }

  public SearchTextSpanHighlighter(LargePreviewProperties largePreviewProperties) {
    this(largePreviewProperties, new SearchHighlightRangeNormalizer());
  }

  SearchTextSpanHighlighter(
      LargePreviewProperties largePreviewProperties,
      SearchHighlightRangeNormalizer highlightRangeNormalizer) {
    this.highlightRangeNormalizer = highlightRangeNormalizer;
  }

  ViewerTextRenderPlan buildRenderPlan(
      String content,
      List<SearchHighlightRange> highlightRanges,
      String baseStyleClass,
      String baseColorHex) {
    if (content == null || content.isEmpty()) {
      return ViewerTextRenderPlan.normal(List.of());
    }

    List<SearchHighlightRange> orderedRanges = highlightRangeNormalizer.normalize(highlightRanges);

    List<ViewerTextRenderFragment> fragments = new java.util.ArrayList<>();
    int cursor = 0;
    for (SearchHighlightRange range : orderedRanges) {
      if (range.startIndex() > cursor) {
        fragments.add(
            buildFragment(
                content.substring(cursor, range.startIndex()),
                baseStyleClass,
                baseColorHex,
                false,
                false));
      }
      if (range.endIndex() > range.startIndex()) {
        fragments.add(
            buildFragment(
                content.substring(range.startIndex(), range.endIndex()),
                baseStyleClass,
                baseColorHex,
                true,
                range.active()));
      }
      cursor = Math.max(cursor, range.endIndex());
    }

    if (cursor < content.length()) {
      fragments.add(
          buildFragment(content.substring(cursor), baseStyleClass, baseColorHex, false, false));
    }
    return ViewerTextRenderPlan.normal(fragments);
  }

  private ViewerTextRenderFragment buildFragment(
      String textValue,
      String baseStyleClass,
      String colorHex,
      boolean highlighted,
      boolean active) {
    return new ViewerTextRenderFragment(textValue, baseStyleClass, colorHex, highlighted, active);
  }

}
