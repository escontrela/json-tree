package com.davidpe.jsontree.application.model;

/**
 * End-to-end curl import outcome after parse, transport execution, and materialization.
 */
public enum CurlDocumentImportStatus {
  IMPORTED,
  INVALID_CURL,
  EXECUTION_FAILED,
  UNSUPPORTED_RESPONSE,
  UNREADABLE_SOURCE
}
