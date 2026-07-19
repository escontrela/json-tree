package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ViewerTextRenderPlanSearchOverlayTest {

  private final ViewerTextRenderPlanSearchOverlay overlay =
      new ViewerTextRenderPlanSearchOverlay(new SearchHighlightRangeNormalizer());

  @Test
  void highlightsOnlyTheMatchedFragmentWhilePreservingBaseStyles() {
    ViewerTextRenderPlan basePlan =
        ViewerTextRenderPlan.normal(
            List.of(
                new ViewerTextRenderFragment("root\n└─ ", "tree-default", "#2d333a", false, false),
                new ViewerTextRenderFragment("name", "tree-key", "#2d333a", false, false),
                new ViewerTextRenderFragment(": ", "tree-default", "#2d333a", false, false),
                new ViewerTextRenderFragment("\"David\"", "tree-string", "#0b8f6a", false, false)));

    ViewerTextRenderPlan highlighted =
        overlay.apply(basePlan, List.of(new SearchHighlightRange(8, 12, true)));

    assertEquals("root\n└─ name: \"David\"", overlay.flatten(highlighted));
    assertEquals("name", highlighted.fragments().get(1).text());
    assertTrue(highlighted.fragments().get(1).highlighted());
    assertTrue(highlighted.fragments().get(1).activeHighlight());
    assertEquals("\"David\"", highlighted.fragments().getLast().text());
    assertFalse(highlighted.fragments().getLast().highlighted());
  }
}
