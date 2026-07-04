# JSON TREE Features

## Imported File Metadata

- The application exposes a dedicated import use case that accepts a local JSON file path.
- Import metadata is normalized into a workflow-friendly result object with absolute path, filename, size, and basic availability flags.
- JavaFX controllers are expected to consume the use case result instead of reading file metadata directly from the filesystem.

## Validation States

- JSON validation is delegated to a dedicated service boundary backed by Jackson parsing.
- Validation outcomes distinguish `VALID`, `INVALID`, `PARSING_ERROR`, and `EMPTY`.
- Invalid payloads include a concise error message plus line and column details when Jackson provides them.

## ASCII Tree Formatting

- Parsed JSON structures can be rendered into an `AsciiTreeDocument` with deterministic branch indentation.
- Object properties render as branches, arrays display their item count, and primitive values render inline.
- Deeply nested objects and arrays preserve alignment so the viewer can remain monospace-friendly.

## Scrollable Viewer

- The main viewer shell contains dedicated ASCII tree content that can be refreshed independently from the empty state.
- Horizontal and vertical scrolling stay enabled for long trees and long lines.
- Rendering a new `AsciiTreeDocument` resets the viewer scroll position to the top-left for a predictable refresh.

## Viewer Visual States

- The main viewer flow exposes explicit `EMPTY`, `DRAGGING`, `LOADING`, `VALID`, and `INVALID` visual states.
- Dragging activates a stronger shell glow, while loading, valid, and invalid states update the status labels consistently.
- Empty-file and invalid outcomes use concise, readable messages inside the viewer shell.

## ASCII Syntax Highlighting

- The viewer highlights structural labels, keys, strings, numbers, booleans, nulls, and array counts with distinct colors in the dark theme.
- Highlighting is applied per text segment inside a monospace `TextFlow`, preserving ASCII alignment.
- If a tree line cannot be tokenized cleanly, the fallback segment remains readable with the default tree color.

## Local History Snapshots

- Dropping a valid `.json` file loads, validates, renders, and stores it as a local snapshot under the configured app-data history directory.
- Snapshot filenames start with a deterministic timestamp and preserve a sanitized version of the original filename.
- History metadata is persisted in a filesystem JSON index so entries can be listed chronologically and reopened later without any database dependency.

## OS Drag And Drop Handoff

- The full main window accepts operating-system file drops and extracts the first supported `.json` path from the transfer payload.
- The controller delegates the dropped path through `ImportJsonUseCase#importFile(Path)` before the workflow continues with validation, rendering, and optional history persistence.
- Unsupported payloads restore a safe viewer state without calling filesystem metadata or parsing APIs directly from the controller.

## Clipboard JSON Import

- `Command+P` or `Command+V` on macOS and `Ctrl+P` or `Ctrl+V` on Windows or Linux now materialize valid clipboard JSON as a temporary local document and route it through the standard validation and rendering workflow.
- Clipboard imports use deterministic temporary filenames, surface `Clipboard` as the active source in metadata and the status rail, and still follow the existing local history snapshot conventions after a successful render.
- Empty, unreadable, or invalid clipboard contents fail without replacing the last valid selected document, while an empty workspace shows a readable clipboard-specific invalid state.

## BMW Theme Tokens

- The shared JavaFX theme now uses a BMW-inspired token baseline: white canvas, dark navy structure, BMW blue primary actions, neutral hairlines, and restrained grayscale body copy.
- Application typography defaults to a BMW-safe sans hierarchy, with the ASCII tree explicitly kept as a monospace rendering exception.
- Shared shell controls no longer rely on glow-heavy borders or pill geometry as the default component language.

## BMW Shell Layout

- The main window now uses an editorial BMW-style shell with a dark structural header, a reorganized metadata band, and restrained rectangular CTA language.
- The ASCII viewer is framed as a dedicated content card with supporting captions instead of floating rounded utility chrome.
- The history screen follows the same shell system so navigation between viewer and archive reads as one product.

## Shared Interactive States

- Validation badges, drag-over states, list selection, focus rings, and button hover/pressed treatments now share one restrained BMW-inspired state model.
- BMW blue is reserved for meaningful emphasis such as active CTA, focused surfaces, drag-ready affordances, and ready/history states.
- The previous purple glow and pill-heavy cues have been removed from the shared shell interactions.

## Two-Column Workspace Shell

- The main window is now distributed as a persistent left utility rail plus a larger right inspection workspace instead of a stacked top-to-bottom shell.
- The screenshot-inspired redistribution is used only for layout grouping; colors, spacing, typography, and surface language continue to follow `DESIGN.md`.
- The left rail keeps space reserved for file context, recent history, and import guidance without reducing the ASCII viewer to a secondary element.

## Left-Rail File Summary

- The active file is now summarized inside the left rail rather than a dedicated top metadata strip.
- The summary card keeps filename, validation badge, file size, loaded timestamp, content type, rendered line count, and source when those values are available.
- With no current file, the summary falls back to a waiting state instead of leaving stale metadata in place.

## Inline Rail History

- The main window now exposes up to ten most recent local snapshots directly inside the left rail for fast reopen.
- Selecting an inline history entry replays the standard reopen flow inside the main viewer, while a `View all` action still routes to the dedicated history screen.
- The import utility block in the rail remains informational only; the whole window is still the true drag-and-drop target.

## Workspace Header And Viewer Controls

- The primary viewer workspace now has a dedicated header with action slots for copy, raw JSON, and search, even when those actions are still placeholder-disabled.
- A secondary viewer toolbar keeps room for display-mode utilities without pushing those stubs into the business workflow.
- The right side also exposes a compact auxiliary viewer aid panel so the screenshot-inspired distribution can exist without copying its dark aesthetic.

## Search Entry Shell

- The main workspace now includes a compact search modal overlay that opens from the existing `Search` action instead of routing to a separate screen.
- The entry panel lives inside the current JavaFX shell and includes a RegExp input plus accept and cancel actions.
- The header layout now reserves a hidden compact strip area to the left of `Copy tree` so accepted searches can later promote into persistent controls without redistributing the top bar again.

## Active Search Strip

- Accepting a search query now promotes a compact utility strip into the workspace header without reopening or replacing the modal.
- The strip occupies the reserved region to the left of `Copy tree` and includes previous, next, occurrence-status, and clear controls as a separate UI state from the entry panel.
- When no search session is active, the strip is fully hidden so the top bar returns to its default editorial layout.

## Search Workflow

- Search execution now lives in an application service that validates RegExp input and runs matches against the current raw JSON source rather than the ASCII-rendered view.
- Invalid expressions surface concise modal errors and do not replace the active header strip state.
- Valid searches create a session with ordered match metadata and active-occurrence state so later highlighting and navigation can build on the same workflow contract.

## Search Highlighting

- The currently selected search occurrence is now highlighted at fragment level rather than by whole line, both in the raw JSON view and in the ASCII tree view.
- Raw JSON highlighting uses the exact match offsets returned by the raw-search workflow, while the ASCII view projects the same match sequence onto its rendered text.
- Clearing the active search session rerenders both viewers without leftover highlight fragments.

## Search Navigation Lifecycle

- Search sessions now track the active occurrence index and expose wrapped previous or next navigation in stable match order.
- The active-search strip shows `current / total` when matches exist and disables navigation controls coherently for zero or one occurrence.
- Moving between matches rerenders the current viewer and scrolls the active highlighted fragment into view; clearing the session hides the strip and restores the viewer to its unhighlighted state.

## Outline Minimap Shell

- The right rail outline panel now exposes a dedicated minimap preview shell instead of only a text placeholder.
- Empty, dragging, loading, valid, invalid, and empty-file flows all update the outline panel coherently inside the existing screen.
- The shell reserves a compact thumbnail surface and keeps the existing outline toggle behavior intact while the interactive minimap feature is layered in.

## Outline Model Generation

- A dedicated application service now derives a compact outline model from the active raw JSON source without depending on JavaFX viewer nodes.
- The outline model keeps structural depth, entry kind, and lightweight visual weight hints so the minimap can render schema shape instead of full-detail content.
- The model is regenerated when a new valid file becomes the current view or when a stored history snapshot is reopened, while ordinary viewer rerenders reuse the current outline state.

## Outline Minimap Rendering

- The outline panel now paints a compact structural minimap into its reserved canvas instead of a static shell placeholder when a valid JSON document is active.
- Rendering uses lightweight sampled rows derived from the outline model so large documents can still produce a readable thumbnail without painting every entry at full detail.
- A visible viewport marker placeholder is now exposed in the minimap shell, ready for later navigation and synchronization work.

## Outline Navigation

- The outline minimap now accepts click and drag interaction to drive the main viewer vertical scroll without replacing the native scrollbars or search navigation flow.
- Coordinate mapping from minimap pointer position to `ScrollPane` scroll value lives in a dedicated helper so the JavaFX controller only wires UI events and current dimensions.
- Short documents that do not require scrolling resolve safely to the top of the viewer instead of producing unstable scroll jumps.

## Outline Viewport Tracking

- The minimap viewport marker now reflects the current visible region of the main viewer instead of staying as a static placeholder.
- Marker updates are deferred to the next JavaFX pulse so scroll changes, viewer rerenders, raw/ASCII toggles, and history reopen flows do not create jittery feedback loops.
- Large files keep the existing sampled minimap rendering and only update marker geometry during scroll, which avoids repainting the full outline on every viewport change.

## Status Rail Synchronization

- The bottom rail now exposes stable technical cues for state, size, rendered line count, and source using only metrics the workflow already knows about.
- Empty, dragging, loading, valid, invalid, and reopened-history flows all update the footer metadata, file summary border treatment, import utility, and viewer shell coherently.
- Reopened snapshots preserve the dedicated history screen flow while still surfacing `History snapshot` as the current source in the main layout.

## History Screen

- The app now includes a dedicated `History` screen registered through `UiScreenId`, `UiScreenFactory`, and `UiFlowManager`.
- The history view lists timestamp, original filename, size, and validation status for stored snapshots.
- When no snapshots exist, the screen falls back to a graceful empty-history message instead of an empty list.

## History Actions

- Clicking a stored history entry reopens it in the main viewer flow.
- Each history row exposes inline deletion without a confirmation modal.
- If the currently open snapshot is deleted from history, the main viewer falls back to a safe empty state on return.

## History Favorites Persistence

- Stored JSON snapshots now support a persisted favorite flag inside the existing file-based history metadata.
- Favorite state is saved and loaded through `metadata.json` without introducing a database and remains safe for older metadata files that do not yet include the new field.
- Updating favorite state preserves the stored snapshot payload and deletion flow so favorites stay coherent with the filesystem-backed history model.

## History Favorite Toggle Workflow

- Toggling favorite state now goes through a dedicated application use case rather than mutating repository state directly from JavaFX cells.
- The workflow exposes explicit outcomes for `favorited`, `unfavorited`, and `missing` entries so UI refresh logic can stay simple and safe.
- Updated favorite state is immediately visible to subsequent history-list reloads through the existing repository abstraction.

## History Row Favorites

- Each stored history row now exposes a dedicated `Pin` or `Pinned` action alongside reopen and delete controls.
- The favorite action delegates through the application-layer toggle use case and refreshes the list without interfering with row-click reopen behavior.
- Pinned rows now surface a clearer visual cue through their button state and a starred title label in the history screen.

## Favorites-Only History Filter

- The history header now includes a toggle inside `storedInspectionsRegion` that switches the list between all stored snapshots and pinned favorites only.
- The toggle state is visually explicit through its label and active styling, and it survives in-screen refreshes such as pinning or deleting entries without persisting across restarts.
- When the favorites-only filter yields no entries, the history screen now shows a dedicated empty-state message instead of falling back to the generic no-history copy.

## Favorites Ordering And Coherence

- The history screen now orders pinned favorites ahead of regular snapshots while preserving the existing chronological ordering inside each group.
- Reopening, deleting, and repeatedly toggling favorites all refresh against the same file-based metadata so the history screen keeps a coherent view without special-case state.
- Regression coverage now exercises favorite persistence, repeat toggles, favorites-only empty states, and the favorites-first ordering rule.

## History Archive Search

- The history screen now exposes a compact text search plus `Search` action inside `storedInspectionsRegion`, alongside the existing import and favorites controls.
- Archive search runs through a dedicated application input port and service that scans both filename metadata and stored JSON snapshot content without moving file traversal logic into the JavaFX controller.
- The feature is available only in the all-history mode; when favorites-only mode is active, the history search controls are hidden and archive search does not execute.
- Blank or whitespace-only search input automatically clears the active history filter and restores the standard archive summary and list state.
- Search-specific empty states now distinguish `no stored history yet` from `no stored JSON matches this query`, while reopen, delete, import, and favorite-toggle interactions continue to refresh coherently after filtering.

## Large Preview Mode

- The workflow now classifies each inspection as `FULL` or `LARGE_PREVIEW` before building the full ASCII tree and before populating JavaFX `TextFlow`, using `json-tree.large-preview.full-render-max-bytes` as the primary gate.
- Oversized files stay on a streaming-safe path: large history reopen flows use stored snapshot paths directly, oversized validation uses streaming Jackson parsing, and the viewer renders a bounded ASCII preview instead of materializing an unlimited tree.
- The bounded preview is explicitly limited by `preview-max-lines`, `preview-max-depth`, and `preview-max-children-per-container`, so huge payloads remain inspectable without pretending to be fully expanded.
- `Raw JSON` and regex search are intentionally disabled in `LARGE_PREVIEW`, while the outline panel stays available through a bounded minimap derived from the visible ASCII preview rather than the full raw JSON payload.
- Even inside allowed modes, the viewer enforces `text-node-budget` guardrails in the syntax-highlighting path. If highlighting would create too many JavaFX text nodes, rendering degrades to simplified plain text instead of risking UI freezes or heap exhaustion.
- The operational goal is resilience, not unlimited rendering. Large-preview support exists to keep oversized JSON inspectable without crashing the app, while ordinary small files keep the richer full-feature path.
