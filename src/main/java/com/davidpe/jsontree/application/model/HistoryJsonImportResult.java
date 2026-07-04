package com.davidpe.jsontree.application.model;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;

public record HistoryJsonImportResult(
    HistoryJsonImportStatus status,
    String message,
    ImportedJsonFile importedEntry
) {

  public HistoryJsonImportResult {
    if ((status == HistoryJsonImportStatus.IMPORTED) != (importedEntry != null)) {
      throw new IllegalArgumentException(
          "Successful history imports must provide an entry, and failures must not.");
    }
  }

  public static HistoryJsonImportResult imported(ImportedJsonFile importedEntry) {
    return new HistoryJsonImportResult(
        HistoryJsonImportStatus.IMPORTED,
        "JSON imported into history successfully.",
        importedEntry);
  }

  public static HistoryJsonImportResult cancelled() {
    return new HistoryJsonImportResult(
        HistoryJsonImportStatus.CANCELLED,
        "History import was cancelled.",
        null);
  }

  public static HistoryJsonImportResult failure(
      HistoryJsonImportStatus status,
      String message) {
    return new HistoryJsonImportResult(status, message, null);
  }

  public boolean successful() {
    return status == HistoryJsonImportStatus.IMPORTED;
  }
}
