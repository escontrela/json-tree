package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViewerTextRenderPlanFactoryTest {

  private final ViewerTextRenderPlanFactory factory =
      new ViewerTextRenderPlanFactory(
          new AsciiTreeSyntaxHighlighter(), new SearchTextSpanHighlighter());

  @Test
  void buildsAsciiPlansThroughTheSharedPipeline() {
    ViewerTextRenderPlan renderPlan =
        factory.buildAsciiPlan(
            new AsciiTreeDocument("root", "root\n└─ count: 2", 2),
            List.of(new SearchHighlightRange(11, 16, true)));

    assertEquals("tree-structure", renderPlan.fragments().getFirst().styleClass());
    assertTrue(renderPlan.fragments().stream().anyMatch(ViewerTextRenderFragment::highlighted));
  }

  @Test
  void buildsRawPlansThroughTheSharedPipeline() {
    ViewerTextRenderPlan renderPlan =
        factory.buildRawPlan(
            "{\n  \"name\" : \"David\"\n}",
            List.of(new SearchHighlightRange(5, 11, true)));

    assertEquals("raw-json-text", renderPlan.fragments().getFirst().styleClass());
    assertTrue(renderPlan.fragments().stream().anyMatch(ViewerTextRenderFragment::highlighted));
  }

  @Test
  void buildsStructurePlansThroughTheSameAsciiPipelineWithoutSearchHighlights() {
    ViewerTextRenderPlan renderPlan =
        factory.buildAsciiPlan(
            new AsciiTreeDocument("root", "root\n└─ users []\n   └─ [0]\n      └─ name", 4),
            List.of());

    assertEquals("tree-structure", renderPlan.fragments().getFirst().styleClass());
    assertTrue(renderPlan.fragments().stream().noneMatch(ViewerTextRenderFragment::highlighted));
  }

  @Test
  void buildsMarkdownPlansThroughTheSharedPipeline() {
    ViewerTextRenderPlan renderPlan =
        factory.buildMarkdownPlan(
            "# Heading\n- item\n> quote\n",
            List.of(new SearchHighlightRange(2, 9, true)));

    assertEquals("markdown-heading", renderPlan.fragments().getFirst().styleClass());
    assertTrue(renderPlan.fragments().stream().anyMatch(ViewerTextRenderFragment::highlighted));
  }
}
