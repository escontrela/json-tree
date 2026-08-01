package com.davidpe.jsontree.ui.support;

/**
 * UI-facing state for the reusable main-window search panel.
 */
public record SearchPanelViewState(
    boolean visible,
    String queryText,
    String occurrenceText,
    String helperText,
    SearchPanelMessageTone helperTone,
    boolean submitEnabled,
    boolean previousEnabled,
    boolean nextEnabled,
    boolean clearEnabled) {

  public static SearchPanelViewState hidden() {
    return new SearchPanelViewState(
        false,
        "",
        "Ready",
        "Use a Java regular expression to search the current viewer.",
        SearchPanelMessageTone.MUTED,
        true,
        false,
        false,
        false);
  }
}
