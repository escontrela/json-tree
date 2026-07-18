package com.davidpe.jsontree.ui.support;

import java.util.List;

public record ViewerTextRenderPlan(boolean guardrailApplied, List<ViewerTextRenderFragment> fragments) {

  public static ViewerTextRenderPlan normal(List<ViewerTextRenderFragment> fragments) {
    return new ViewerTextRenderPlan(false, List.copyOf(fragments));
  }

  public static ViewerTextRenderPlan guardrailFallback(
      String content, String styleClass, String colorHex) {
    return new ViewerTextRenderPlan(
        true,
        List.of(new ViewerTextRenderFragment(content, styleClass, colorHex, false, false)));
  }
}
