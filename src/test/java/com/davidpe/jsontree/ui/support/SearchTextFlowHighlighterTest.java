package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchTextFlowHighlighterTest {

  @Test
  void keepsNormalHighlightingWithinBudget() {
    SearchTextFlowHighlighter highlighter = new SearchTextFlowHighlighter();

    TextFlowRenderPlan renderPlan =
        highlighter.buildRenderPlan(
            "alpha beta gamma",
            List.of(new SearchHighlightRange(6, 10, true)),
            "raw-json-text",
            "#2d333a");

    assertFalse(renderPlan.guardrailApplied());
    assertEquals(3, renderPlan.fragments().size());
  }

  @Test
  void fallsBackToSinglePlainTextNodeWhenBudgetWouldBeExceeded() {
    LargePreviewProperties properties = new LargePreviewProperties();
    properties.setTextNodeBudget(2);
    SearchTextFlowHighlighter highlighter = new SearchTextFlowHighlighter(properties);
    String content = "alpha beta gamma delta";

    TextFlowRenderPlan renderPlan =
        highlighter.buildRenderPlan(
            content,
            List.of(
                new SearchHighlightRange(0, 5, false),
                new SearchHighlightRange(6, 10, true),
                new SearchHighlightRange(11, 16, false)),
            "raw-json-text",
            "#2d333a");

    assertTrue(renderPlan.guardrailApplied());
    assertEquals(1, renderPlan.fragments().size());
    assertEquals(content, renderPlan.fragments().getFirst().text());
  }
}
