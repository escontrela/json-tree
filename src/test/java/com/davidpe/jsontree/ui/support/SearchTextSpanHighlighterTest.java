package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchTextSpanHighlighterTest {

  @Test
  void keepsNormalHighlightingWithinBudget() {
    SearchTextSpanHighlighter highlighter = new SearchTextSpanHighlighter();

    ViewerTextRenderPlan renderPlan =
        highlighter.buildRenderPlan(
            "alpha beta gamma",
            List.of(new SearchHighlightRange(6, 10, true)),
            "raw-json-text",
            "#2d333a");

    assertFalse(renderPlan.guardrailApplied());
    assertEquals(3, renderPlan.fragments().size());
  }

  @Test
  void keepsSplitHighlightSpansEvenWhenTheLegacyBudgetWouldHaveBeenExceeded() {
    LargePreviewProperties properties = new LargePreviewProperties();
    SearchTextSpanHighlighter highlighter = new SearchTextSpanHighlighter(properties);
    String content = "alpha beta gamma delta";

    ViewerTextRenderPlan renderPlan =
        highlighter.buildRenderPlan(
            content,
            List.of(
                new SearchHighlightRange(0, 5, false),
                new SearchHighlightRange(6, 10, true),
                new SearchHighlightRange(11, 16, false)),
            "raw-json-text",
            "#2d333a");

    assertFalse(renderPlan.guardrailApplied());
    assertTrue(renderPlan.fragments().size() > 1);
    assertEquals(
        content,
        renderPlan.fragments().stream()
            .map(ViewerTextRenderFragment::text)
            .reduce("", String::concat));
  }

  @Test
  void normalizesOverlappingHighlightRangesBeforeRendering() {
    SearchTextSpanHighlighter highlighter = new SearchTextSpanHighlighter();

    ViewerTextRenderPlan renderPlan =
        highlighter.buildRenderPlan(
            "alpha beta gamma",
            List.of(
                new SearchHighlightRange(6, 10, false),
                new SearchHighlightRange(8, 14, true)),
            "raw-json-text",
            "#2d333a");

    assertFalse(renderPlan.guardrailApplied());
    assertEquals("be", renderPlan.fragments().get(1).text());
    assertFalse(renderPlan.fragments().get(1).activeHighlight());
    assertEquals("ta gam", renderPlan.fragments().get(2).text());
    assertTrue(renderPlan.fragments().get(2).activeHighlight());
  }
}
