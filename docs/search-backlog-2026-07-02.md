# Search Backlog — 2026-07-02

Tasker was not reachable from this session, so these backlog-ready tasks are recorded here verbatim for the next synced run.

## TREE-0023 — Search modal shell and top-slot integration

Status: backlog

Goal

Create the JavaFX search entry flow so pressing the `Search` button opens a small modal search panel for entering a regular expression.

User Story

As a developer, I want a compact search modal to appear when I press `Search`, so that I can enter a RegExp without leaving the main JSON workspace.

Acceptance Criteria

* Pressing the `Search` button opens a small modal-style panel rather than a full-screen view.
* The panel contains a text input for the RegExp plus `Accept` and `Cancel` actions.
* The layout is prepared so that, after a search is accepted, the compact active-search controls can appear in the empty top area to the left of `Copy Tree`.
* Cancel closes the modal without changing the current viewer state.
* The layout follows `DESIGN.md` for styling and uses the existing JavaFX shell instead of introducing a new screen.

Out of Scope

* Regex validation logic.
* Match highlighting.
* Search navigation across occurrences.

## TREE-0024 — Active search control strip in top workspace header

Status: backlog

Goal

Add the persistent compact search control strip that appears only after a query is accepted, using the top region to the left of `Copy Tree`.

User Story

As a developer, I want accepted searches to expose a compact active-search strip near the top actions, so that I can navigate and clear results without reopening the entry modal every time.

Acceptance Criteria

* After a search is accepted, a compact top search strip becomes visible in the region to the left of `Copy Tree`.
* The strip includes previous and next buttons, an occurrences label, and a clear/cancel search action.
* The strip stays hidden when no search is active.
* The strip is designed as a small utility panel rather than a second large modal.
* The search entry modal and the active search strip are treated as distinct UI states with coherent transitions.

Out of Scope

* Actual regex execution.
* Highlight rendering.
* Search result scrolling behavior.

## TREE-0025 — Regex validation and raw JSON search engine

Status: backlog

Goal

Implement the application-layer search workflow that validates a RegExp and executes the search over the raw JSON source.

User Story

As a developer, I want the entered RegExp validated and executed against the raw JSON content, so that search behavior is reliable before any viewer highlighting is applied.

Acceptance Criteria

* Accepting a search query validates whether the entered RegExp is syntactically correct.
* Invalid expressions show a concise readable error and do not activate the search strip.
* Valid expressions execute against the raw JSON representation, not against already formatted ASCII text.
* The search workflow returns all occurrences and enough metadata to drive highlighting and next/previous navigation.
* The implementation keeps search logic out of the JavaFX controller and places it in an application/service layer suitable for tests.

Out of Scope

* Highlight painting in the raw or ASCII viewer.
* UX animation or advanced search options.
* Search persistence across application restarts.

## TREE-0026 — Highlight current and matching fragments in raw and ASCII views

Status: backlog

Goal

Highlight the matched search fragment in whichever viewer is active, whether the user is currently in raw JSON mode or ASCII tree mode.

User Story

As a developer, I want only the matched fragment to be visually highlighted in the current viewer, so that I can identify search hits immediately without losing context.

Acceptance Criteria

* When a valid search finds matches, the currently selected occurrence is visibly highlighted in the active view.
* Highlighting affects only the matched fragment, not the entire line or surrounding document unnecessarily.
* Highlighting works both in raw JSON mode and in ASCII tree mode, using the same search result set from the raw JSON search engine.
* If multiple occurrences exist, the first selected occurrence is highlighted automatically after search acceptance.
* Clearing the search removes all highlighting cleanly from whichever view is visible.

Out of Scope

* Navigation button behavior beyond selecting a new match.
* New search syntax beyond regular expressions.
* Rewriting the existing renderers unless needed for highlight support.

## TREE-0027 — Search navigation, match count, and clear-session lifecycle

Status: backlog

Goal

Complete the search experience with previous/next navigation, current occurrence tracking, and a clean way to end the active search session.

User Story

As a developer, I want to move through all matches and then clear the active search session, so that repeated search results remain manageable inside long JSON documents.

Acceptance Criteria

* Previous and next buttons move between occurrences in a stable order.
* The top strip shows occurrence information such as current position and total matches.
* Moving between matches updates the highlight and scroll/focus behavior so the active occurrence becomes visible.
* The clear/cancel search action removes the highlight state, hides the active search strip, and returns the workspace to its normal state.
* The flow behaves coherently when there are zero matches, one match, or many matches.

Out of Scope

* Advanced replacement features.
* Multi-query history.
* Persisting the last search across files or sessions.
