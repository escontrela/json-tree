package com.davidpe.jsontree.ui.support;

/**
 * UI-facing shortcut descriptor used by Settings and any other surface that needs to present the
 * currently supported keyboard actions.
 */
public record SupportedShortcut(String title, String description, String chordLabel) {}
