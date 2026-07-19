package com.davidpe.jsontree.application.model;

/**
 * Result states for drag-and-drop imports that may resolve to local files or curl text files.
 */
public enum DroppedFileImportStatus {
  IMPORTED,
  UNSUPPORTED_DROP,
  UNREADABLE_FILE,
  INVALID_CURL,
  EXECUTION_FAILED,
  UNSUPPORTED_RESPONSE
}
