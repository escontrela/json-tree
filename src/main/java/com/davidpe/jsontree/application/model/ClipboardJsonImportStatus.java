package com.davidpe.jsontree.application.model;

public enum ClipboardJsonImportStatus {
  IMPORTED,
  EMPTY_CLIPBOARD,
  UNREADABLE_CLIPBOARD,
  INVALID_JSON,
  INVALID_CURL,
  CURL_EXECUTION_FAILED,
  UNSUPPORTED_RESPONSE
}
