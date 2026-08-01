package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonSearchMatch;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelMessageTone;
import com.davidpe.jsontree.ui.controls.search.model.SearchPanelViewState;
import com.davidpe.jsontree.ui.controls.search.support.SearchPanelViewStateResolver;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchPanelViewStateResolverTest {

  private final SearchPanelViewStateResolver resolver = new SearchPanelViewStateResolver();

  @Test
  void returnsHiddenStateForTheClosedPanel() {
    SearchPanelViewState state = resolver.hidden();

    assertFalse(state.visible());
    assertEquals("Ready", state.occurrenceText());
  }

  @Test
  void keepsTheRegexOnlyIdleContractVisible() {
    SearchPanelViewState state = resolver.idle(true, "user.*", "Java regular expression search. Literal fallback is disabled.");

    assertEquals("Java regular expression search. Literal fallback is disabled.", state.helperText());
    assertEquals(SearchPanelMessageTone.MUTED, state.helperTone());
  }

  @Test
  void returnsErrorStateForInvalidRegexFeedback() {
    SearchPanelViewState state = resolver.invalid(true, "foo(", "Dangling group");

    assertTrue(state.visible());
    assertEquals(SearchPanelMessageTone.ERROR, state.helperTone());
    assertEquals("Dangling group", state.helperText());
    assertTrue(state.clearEnabled());
  }

  @Test
  void returnsActiveNavigationStateForARealSearchSession() {
    JsonSearchSession session =
        new JsonSearchSession(
            "source",
            "admin",
            List.of(
                new JsonSearchMatch(4, 9, "admin"),
                new JsonSearchMatch(12, 17, "admin")),
            1);

    SearchPanelViewState state = resolver.active(true, session);

    assertEquals("admin", state.queryText());
    assertEquals("2 of 2", state.occurrenceText());
    assertTrue(state.previousEnabled());
    assertTrue(state.nextEnabled());
    assertEquals(SearchPanelMessageTone.ACCENT, state.helperTone());
  }

  @Test
  void returnsZeroMatchMessagingWithoutNavigation() {
    JsonSearchSession session =
        new JsonSearchSession("source", "missing", List.of(), -1);

    SearchPanelViewState state = resolver.active(true, session);

    assertEquals("0 matches", state.occurrenceText());
    assertEquals(
        "Valid regex, but no matches were found in the current viewer.",
        state.helperText());
    assertEquals(SearchPanelMessageTone.ACCENT, state.helperTone());
    assertFalse(state.previousEnabled());
    assertFalse(state.nextEnabled());
  }
}
