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
- Highlighting is applied per text segment inside the shared monospace `RichTextFX` viewer, preserving ASCII alignment without recreating large node trees.
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

## RichTextFX Viewer Runtime

- The main viewer now uses one shared read-only `RichTextFX` surface for ASCII tree, raw JSON, and large-preview chunk rendering instead of maintaining parallel `TextFlow` scene graphs.
- Syntax coloring, raw-view highlighting, and active search emphasis now flow through virtualized style spans, preserving monospace alignment while avoiding the old medium-file node explosion path.
- Large-preview behavior stays on the existing bounded chunk strategy, so oversized files still avoid full viewer materialization while reusing the same viewer pipeline as small files.

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

- The workflow now classifies each inspection as `FULL` or `LARGE_PREVIEW` before building the full ASCII tree and before populating the shared viewer surface, using `json-tree.large-preview.full-render-max-bytes` as a strict `>` primary gate.
- Oversized files now stay on a byte-paginated path: the large session builds an offset index first, then loads bounded chunks directly from the original file on demand instead of materializing a full ASCII tree in memory.
- The visible chunk is capped by byte budget rather than line budget, so huge payloads remain inspectable without pretending that the full document is currently expanded or resident.
- `LARGE_PREVIEW` now stays fixed on the current page raw view. The app always attempts local Jackson pretty-print when the active chunk parses cleanly as standalone JSON.
- A persisted `Pretty on large preview` setting can additionally enable a deterministic best-effort formatter for incomplete standalone chunks; when disabled, the old plain-raw fallback remains in place.
- Regex search and the outline/minimap are intentionally disabled in this variant of `LARGE_PREVIEW`.
- Because that outline rail is non-functional in byte-paged `LARGE_PREVIEW`, the right-side panel now starts hidden for large files. The `Outline` button stays available so the user can still reopen the unavailable shell manually if they want the extra context rail visible.
- Even inside allowed modes, the viewer enforces `text-node-budget` guardrails in the syntax-highlighting path. If highlighting would create too many JavaFX text nodes, rendering degrades to simplified plain text instead of risking UI freezes or heap exhaustion.
- The operational goal is resilience, not unlimited rendering. Large-preview support exists to keep oversized JSON inspectable without crashing the app, while ordinary small files keep the richer full-feature path.

## Paged Large Preview Sessions

- A dedicated application-level paged-session contract now exists for the next evolution of `LARGE_PREVIEW`, separate from the ordinary `FULL` `AsciiTreeDocument` result path.
- The session model tracks stable session identity, normalized source identity, current page, total pages, total source bytes, byte-window metadata, and page readiness state without leaking JavaFX concerns into `application` or `domain`.
- The contract now also carries coarse source checkpoints so the app can reopen or advance through huge files with deterministic byte offsets while keeping the resident cache bounded.

## Large Preview Global Ranges

- The paged large-preview session contract now exposes document-wide byte ranges for each chunk in addition to page indexes.
- Each session also carries explicit resident-cache radius metadata, so the workflow can reason about bounded nearby-page residency as part of the session state instead of relying on controller heuristics.

## Large Preview Page Materialization

- Oversized JSON rendering now has a byte-index materialization path that creates ordered chunk descriptors instead of persisting rendered ASCII pages.
- Each chunk descriptor carries deterministic access metadata, including page index, source byte offset, visible byte count, and overlap bytes used to make adjacent navigation feel continuous.
- The large-session workflow now enters interactive mode as soon as the session index is known, because chunk text is loaded lazily from the original file.

## Large Preview Global Scroll Workflow

- Large-mode activation no longer uses a global oversized-document scrollbar. The main `ScrollPane` only scrolls inside the current chunk.
- Vertical scroll never changes the active page in this variant. Scrolling is intentionally local to the page currently loaded in the viewer.
- Page changes are explicit through `Previous` and `Next`, which keeps large-file navigation deterministic and removes edge-scroll page jumps.

## Large Preview Page Controls

- Large mode now exposes a compact page strip in the main viewer toolbar with `Previous`, `Next`, the current page label, and the known total page count.
- The controls stay hidden for ordinary `FULL` rendering, disable themselves at the first and last large-preview pages, and become the only way to change page in byte-paged large preview.
- Because the strip state is resolved from the active session rather than inferred from JavaFX widgets, button navigation stays synchronized on the same current page identity used by raw-page rendering.

## Large Preview Full Activation

- The current oversized workflow treats a large-preview session as interactive once the byte index and total page count are known.
- This keeps `FULL` unchanged for small files while allowing oversized files to open without building the rendered tree or the full raw payload in heap memory.

## Large Preview Outline Digest

- This byte-paginated large-preview variant does not build or expose an outline digest.
- The outline/minimap rail is intentionally disabled so the workflow can focus on stable chunk navigation, resident-cache control, and current-chunk raw access.

## Large Preview Outline Navigation

- The outline/minimap is disabled in the current byte-paginated large-preview workflow.
- Large navigation is intentionally reduced to `Previous` and `Next` until a dedicated follow-up reintroduces a visual navigation aid on top of byte-based sessions.

## Large Preview Loading Affordance

- The main viewer keeps a compact four-square loading affordance reserved for cold large-preview chunk transitions that outlive a short reveal delay, so warm in-memory swaps stay visually silent.
- Initial oversized imports and history reopens still execute off the JavaFX thread, which lets the same square-based CLI-style cadence appear during byte-index creation instead of freezing the shell.
- Ordinary `FULL` rendering and hot large-mode chunk swaps that complete before the reveal delay do not activate the overlay.

## Large Preview Operational Defaults

- The paged large-preview workflow keeps `FULL` untouched for small files and applies the oversized path with a default coarse index stride of `512 KB`, a visible chunk budget of `150 KB`, and a small overlap between adjacent chunks.
- The hot cache window is now configurable through `json-tree.large-preview.warm-page-radius`, defaults to `20`, and is clamped to a safe upper bound so oversized sessions cannot request an unbounded resident page window by configuration mistake.
- Residency is always calculated from the same current-page state, and chunks outside `current - radius` through `current + radius` are evicted from memory while remaining reloadable from the original source file.
- Temporary paged-session storage is cleaned when the active oversized file is replaced, when the session is discarded, and when the application tears down the paged workflow at shutdown.
- Large mode remains intentionally stream-safe rather than a byte-for-byte clone of the small-file renderer. The viewer stays on raw current-page chunks, while regex search and outline/minimap stay disabled until a dedicated follow-up ticket changes that contract.

## Settings Screen

- The top toolbar now exposes a dedicated `Settings` screen integrated through the normal `UiScreenId` and `UiFlowManager` flow instead of a modal dialog.
- Settings currently edit two runtime values: the large-preview activation threshold and the byte-paged visible chunk size.
- Settings also expose `Pretty on large preview`, a persisted toggle that enables best-effort formatting for invalid standalone large-preview raw chunks on future JSON loads.
- `Back` always discards unsaved form edits and returns to the main screen.
- `Apply` persists the edited values to local file-based settings storage and updates the runtime snapshot used by the next JSON import or history reopen.
- The currently opened document is not reprocessed in place after `Apply`; the new values start affecting the next load only.
- The screen also shows the JVM startup memory reference and highlights the threshold advisory in red when the configured large-file threshold exceeds that startup reference.
