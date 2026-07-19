package com.davidpe.jsontree.ui.support;

/**
 * UI-facing state for the settings form.
 */
public record SettingsFormState(
    String thresholdText,
    String chunkText,
    boolean prettyLargePreviewSelected,
    boolean nightModeSelected,
    String memoryReferenceText,
    String warningText,
    boolean warningActive,
    String thresholdErrorText,
    String chunkErrorText,
    boolean applyEnabled) {}
