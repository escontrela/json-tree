package com.davidpe.jsontree.ui.support;

public record TextFlowRenderFragment(
    String text,
    String styleClass,
    String colorHex,
    boolean highlighted,
    boolean activeHighlight) {}
