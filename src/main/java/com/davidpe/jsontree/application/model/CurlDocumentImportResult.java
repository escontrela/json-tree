package com.davidpe.jsontree.application.model;

/**
 * Result of importing a curl-backed response into the normal viewer workflow.
 */
public record CurlDocumentImportResult(
    CurlDocumentImportStatus status, String message, JsonViewerLoadResult loadResult) {

  public static CurlDocumentImportResult imported(JsonViewerLoadResult loadResult) {
    return new CurlDocumentImportResult(
        CurlDocumentImportStatus.IMPORTED, "Curl response imported successfully.", loadResult);
  }

  public static CurlDocumentImportResult failure(CurlDocumentImportStatus status, String message) {
    return new CurlDocumentImportResult(status, message, null);
  }

  public boolean successful() {
    return status == CurlDocumentImportStatus.IMPORTED;
  }
}
