package com.davidpe.jsontree.ui.support;

public record TextFlowRenderOutcome(boolean guardrailApplied) {

  public static TextFlowRenderOutcome normalOutcome() {
    return new TextFlowRenderOutcome(false);
  }

  public static TextFlowRenderOutcome guardrailTriggered() {
    return new TextFlowRenderOutcome(true);
  }
}
