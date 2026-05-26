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
