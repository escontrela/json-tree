package com.davidpe.jsontree.application.model;

public record ClipboardJsonImportResult(
    ClipboardJsonImportStatus status,
    String message,
    JsonViewerLoadResult loadResult
) {

  public ClipboardJsonImportResult {
    if ((status == ClipboardJsonImportStatus.VALID_JSON) != (loadResult != null)) {
      throw new IllegalArgumentException(
          "Successful clipboard imports must provide a load result, and failures must not.");
    }
  }

  public static ClipboardJsonImportResult success(JsonViewerLoadResult loadResult) {
    return new ClipboardJsonImportResult(
        ClipboardJsonImportStatus.VALID_JSON,
        "Clipboard JSON imported successfully.",
        loadResult);
  }

  public static ClipboardJsonImportResult failure(
      ClipboardJsonImportStatus status,
      String message
  ) {
    return new ClipboardJsonImportResult(status, message, null);
  }

  public boolean successful() {
    return status == ClipboardJsonImportStatus.VALID_JSON;
  }
}
