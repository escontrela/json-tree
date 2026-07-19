package com.davidpe.jsontree.application.model;

/**
 * UI-facing result for dropped-file imports.
 */
public record DroppedFileImportResult(
    DroppedFileImportStatus status, String message, JsonViewerLoadResult loadResult) {

  public static DroppedFileImportResult imported(JsonViewerLoadResult loadResult) {
    return new DroppedFileImportResult(
        DroppedFileImportStatus.IMPORTED, "Dropped file imported successfully.", loadResult);
  }

  public static DroppedFileImportResult failure(DroppedFileImportStatus status, String message) {
    return new DroppedFileImportResult(status, message, null);
  }

  public boolean successful() {
    return status == DroppedFileImportStatus.IMPORTED;
  }
}
