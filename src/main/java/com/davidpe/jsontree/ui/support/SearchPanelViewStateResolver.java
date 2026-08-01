package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonSearchSession;
import org.springframework.stereotype.Component;

/**
 * Maps search workflow state into the floating panel presentation model.
 */
@Component
public class SearchPanelViewStateResolver {

  public SearchPanelViewState hidden() {
    return SearchPanelViewState.hidden();
  }

  public SearchPanelViewState idle(boolean visible, String queryText) {
    return new SearchPanelViewState(
        visible,
        safeQuery(queryText),
        "Ready",
        "Use a Java regular expression to search the current viewer.",
        SearchPanelMessageTone.MUTED,
        true,
        false,
        false,
        false);
  }

  public SearchPanelViewState invalid(boolean visible, String queryText, String errorText) {
    return new SearchPanelViewState(
        visible,
        safeQuery(queryText),
        "Regex error",
        safeError(errorText),
        SearchPanelMessageTone.ERROR,
        true,
        false,
        false,
        true);
  }

  public SearchPanelViewState active(boolean visible, JsonSearchSession session) {
    boolean hasMatches = session != null && session.hasMatches();
    boolean hasMultipleMatches = session != null && session.totalMatches() > 1;
    return new SearchPanelViewState(
        visible,
        session == null ? "" : session.query(),
        formatOccurrence(session),
        hasMatches
            ? "Search session active in the current viewer."
            : "No matches found in the current viewer.",
        hasMatches ? SearchPanelMessageTone.ACCENT : SearchPanelMessageTone.MUTED,
        true,
        hasMultipleMatches,
        hasMultipleMatches,
        true);
  }

  private String formatOccurrence(JsonSearchSession session) {
    if (session == null) {
      return "Ready";
    }
    if (!session.hasMatches()) {
      return "0 matches";
    }
    return (session.activeMatchIndex() + 1) + " / " + session.totalMatches();
  }

  private String safeQuery(String queryText) {
    return queryText == null ? "" : queryText;
  }

  private String safeError(String errorText) {
    return (errorText == null || errorText.isBlank())
        ? "The current regular expression is not valid."
        : errorText;
  }
}
