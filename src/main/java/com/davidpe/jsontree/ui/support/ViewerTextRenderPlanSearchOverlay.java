package com.davidpe.jsontree.ui.support;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reprojects search highlight ranges over an existing render plan while preserving each fragment's
 * base style and color.
 */
@Component
public class ViewerTextRenderPlanSearchOverlay {

  private final SearchHighlightRangeNormalizer highlightRangeNormalizer;

  public ViewerTextRenderPlanSearchOverlay(
      SearchHighlightRangeNormalizer highlightRangeNormalizer) {
    this.highlightRangeNormalizer = highlightRangeNormalizer;
  }

  public ViewerTextRenderPlan apply(
      ViewerTextRenderPlan basePlan, List<SearchHighlightRange> highlightRanges) {
    if (basePlan == null || basePlan.fragments().isEmpty()) {
      return ViewerTextRenderPlan.normal(List.of());
    }

    List<SearchHighlightRange> ranges = highlightRangeNormalizer.normalize(highlightRanges);
    if (ranges.isEmpty()) {
      return ViewerTextRenderPlan.normal(basePlan.fragments());
    }

    List<ViewerTextRenderFragment> fragments = new ArrayList<>();
    int globalOffset = 0;
    for (ViewerTextRenderFragment fragment : basePlan.fragments()) {
      String text = fragment.text() == null ? "" : fragment.text();
      if (text.isEmpty()) {
        continue;
      }

      int fragmentStart = globalOffset;
      int fragmentEnd = globalOffset + text.length();
      int localCursor = 0;
      for (SearchHighlightRange range : ranges) {
        if (range.endIndex() <= fragmentStart) {
          continue;
        }
        if (range.startIndex() >= fragmentEnd) {
          break;
        }

        int localStart = Math.max(0, range.startIndex() - fragmentStart);
        int localEnd = Math.min(text.length(), range.endIndex() - fragmentStart);
        if (localStart > localCursor) {
          fragments.add(copyFragment(fragment, text.substring(localCursor, localStart), false, false));
        }
        if (localEnd > localStart) {
          fragments.add(copyFragment(fragment, text.substring(localStart, localEnd), true, range.active()));
        }
        localCursor = Math.max(localCursor, localEnd);
      }

      if (localCursor < text.length()) {
        fragments.add(copyFragment(fragment, text.substring(localCursor), false, false));
      }
      globalOffset = fragmentEnd;
    }

    return ViewerTextRenderPlan.normal(fragments);
  }

  public String flatten(ViewerTextRenderPlan renderPlan) {
    if (renderPlan == null || renderPlan.fragments().isEmpty()) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (ViewerTextRenderFragment fragment : renderPlan.fragments()) {
      if (fragment.text() != null) {
        builder.append(fragment.text());
      }
    }
    return builder.toString();
  }

  private ViewerTextRenderFragment copyFragment(
      ViewerTextRenderFragment baseFragment,
      String text,
      boolean highlighted,
      boolean activeHighlight) {
    return new ViewerTextRenderFragment(
        text,
        baseFragment.styleClass(),
        baseFragment.colorHex(),
        highlighted,
        activeHighlight);
  }
}
