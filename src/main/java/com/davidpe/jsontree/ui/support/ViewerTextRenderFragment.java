package com.davidpe.jsontree.ui.support;

public record ViewerTextRenderFragment(
    String text,
    String styleClass,
    String colorHex,
    boolean highlighted,
    boolean activeHighlight) {}
