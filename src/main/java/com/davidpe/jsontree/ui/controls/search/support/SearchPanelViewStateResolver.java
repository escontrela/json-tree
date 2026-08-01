package com.davidpe.jsontree.ui.controls.search.support;

import com.davidpe.jsontree.application.model.JsonSearchSession;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelCropState;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelMessageTone;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelViewState;
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
    return idle(
        visible,
        queryText,
        "Java regular expression search. Literal fallback is disabled.");
  }

  public SearchPanelViewState idle(boolean visible, String queryText, String helperText) {
    return idle(visible, queryText, helperText, SearchPanelCropState.hidden());
  }

  public SearchPanelViewState idle(
      boolean visible, String queryText, String helperText, SearchPanelCropState cropState) {
    return new SearchPanelViewState(
        visible,
        safeQuery(queryText),
        "Ready",
        safeHelper(helperText),
        SearchPanelMessageTone.MUTED,
        true,
        false,
        false,
        false,
        safeCropState(cropState));
  }

  public SearchPanelViewState invalid(boolean visible, String queryText, String errorText) {
    return invalid(visible, queryText, errorText, SearchPanelCropState.hidden());
  }

  public SearchPanelViewState invalid(
      boolean visible, String queryText, String errorText, SearchPanelCropState cropState) {
    return new SearchPanelViewState(
        visible,
        safeQuery(queryText),
        "Regex error",
        safeError(errorText),
        SearchPanelMessageTone.ERROR,
        true,
        false,
        false,
        true,
        safeCropState(cropState));
  }

  public SearchPanelViewState active(boolean visible, JsonSearchSession session) {
    return active(visible, session, SearchPanelCropState.hidden());
  }

  public SearchPanelViewState active(
      boolean visible, JsonSearchSession session, SearchPanelCropState cropState) {
    boolean hasMatches = session != null && session.hasMatches();
    boolean hasMultipleMatches = session != null && session.totalMatches() > 1;
    return new SearchPanelViewState(
        visible,
        session == null ? "" : session.query(),
        formatOccurrence(session),
        hasMatches
            ? "Search session active in the current viewer."
            : "Valid regex, but no matches were found in the current viewer.",
        SearchPanelMessageTone.ACCENT,
        true,
        hasMultipleMatches,
        hasMultipleMatches,
        true,
        safeCropState(cropState));
  }

  private String formatOccurrence(JsonSearchSession session) {
    if (session == null) {
      return "Ready";
    }
    if (!session.hasMatches()) {
      return "0 matches";
    }
    return (session.activeMatchIndex() + 1) + " of " + session.totalMatches();
  }

  private String safeQuery(String queryText) {
    return queryText == null ? "" : queryText;
  }

  private String safeError(String errorText) {
    return (errorText == null || errorText.isBlank())
        ? "The current regular expression is not valid."
        : errorText;
  }

  private String safeHelper(String helperText) {
    return (helperText == null || helperText.isBlank())
        ? "Java regular expression search. Literal fallback is disabled."
        : helperText;
  }

  private SearchPanelCropState safeCropState(SearchPanelCropState cropState) {
    return cropState == null ? SearchPanelCropState.hidden() : cropState;
  }
}
