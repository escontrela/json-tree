package com.davidpe.jsontree.ui.support;

import java.util.List;

public record TextFlowRenderPlan(boolean guardrailApplied, List<TextFlowRenderFragment> fragments) {

  public static TextFlowRenderPlan normal(List<TextFlowRenderFragment> fragments) {
    return new TextFlowRenderPlan(false, List.copyOf(fragments));
  }

  public static TextFlowRenderPlan guardrailFallback(
      String content, String styleClass, String colorHex) {
    return new TextFlowRenderPlan(
        true,
        List.of(new TextFlowRenderFragment(content, styleClass, colorHex, false, false)));
  }
}
