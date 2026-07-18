package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViewerTextRenderPlanFactoryTest {

  private final ViewerTextRenderPlanFactory factory =
      new ViewerTextRenderPlanFactory(
          new AsciiTreeSyntaxHighlighter(), new SearchTextFlowHighlighter());

  @Test
  void buildsAsciiPlansThroughTheSharedPipeline() {
    TextFlowRenderPlan renderPlan =
        factory.buildAsciiPlan(
            new AsciiTreeDocument("root", "root\n└─ count: 2", 2),
            List.of(new SearchHighlightRange(11, 16, true)));

    assertEquals("tree-structure", renderPlan.fragments().getFirst().styleClass());
    assertTrue(renderPlan.fragments().stream().anyMatch(TextFlowRenderFragment::highlighted));
  }

  @Test
  void buildsRawPlansThroughTheSharedPipeline() {
    TextFlowRenderPlan renderPlan =
        factory.buildRawPlan(
            "{\n  \"name\" : \"David\"\n}",
            List.of(new SearchHighlightRange(5, 11, true)));

    assertEquals("raw-json-text", renderPlan.fragments().getFirst().styleClass());
    assertTrue(renderPlan.fragments().stream().anyMatch(TextFlowRenderFragment::highlighted));
  }
}
