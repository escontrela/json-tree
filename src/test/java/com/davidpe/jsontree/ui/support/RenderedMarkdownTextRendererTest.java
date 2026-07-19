package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RenderedMarkdownTextRendererTest {

  private final RenderedMarkdownTextRenderer renderer = new RenderedMarkdownTextRenderer();

  @Test
  void rendersReadableMarkdownBlocksWithoutUsingTheRawMarkersVerbatim() {
    ViewerTextRenderPlan renderPlan =
        renderer.buildRenderPlan(
            """
            # Heading
            - item
            7. numbered
            > quote
            ```json
            {"ok":true}
            ```
            Plain paragraph
            """);

    assertEquals("Heading\n", renderPlan.fragments().get(0).text());
    assertEquals("markdown-rendered-heading-1", renderPlan.fragments().get(0).styleClass());
    assertTrue(
        renderPlan.fragments().stream()
            .anyMatch(fragment -> "• item\n".equals(fragment.text())));
    assertTrue(
        renderPlan.fragments().stream()
            .anyMatch(fragment -> "7. numbered\n".equals(fragment.text())));
    assertTrue(
        renderPlan.fragments().stream()
            .anyMatch(fragment -> "│ quote\n".equals(fragment.text())));
    assertTrue(
        renderPlan.fragments().stream()
            .anyMatch(fragment -> "code block (json)\n".equals(fragment.text())));
    assertTrue(
        renderPlan.fragments().stream()
            .anyMatch(fragment -> "markdown-rendered-code".equals(fragment.styleClass())));
  }
}
