package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownTextSyntaxHighlighterTest {

  private final MarkdownTextSyntaxHighlighter highlighter = new MarkdownTextSyntaxHighlighter();

  @Test
  void stylesCommonMarkdownSourceMarkersWithoutChangingRawText() {
    ViewerTextRenderPlan renderPlan =
        highlighter.buildRenderPlan(
            """
            # Heading
            - item
            > quote
            ```
            code
            ```
            """);

    assertEquals("markdown-heading", renderPlan.fragments().get(0).styleClass());
    assertTrue(
        renderPlan.fragments().stream()
            .anyMatch(fragment -> "markdown-list".equals(fragment.styleClass())));
    assertTrue(
        renderPlan.fragments().stream()
            .anyMatch(fragment -> "markdown-quote".equals(fragment.styleClass())));
    assertTrue(
        renderPlan.fragments().stream()
            .anyMatch(fragment -> "markdown-fence".equals(fragment.styleClass())));
  }
}
