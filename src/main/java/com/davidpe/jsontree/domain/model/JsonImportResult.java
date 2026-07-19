package com.davidpe.jsontree.domain.model;

import java.nio.file.Path;

/**
 * Result of an import/read operation over a JSON source.
 *
 * <p>This record models process state (what was found and whether it can be used) and lightweight
 * metadata used by workflows, such as source type and file accessibility. Unlike {@link
 * ImportedJsonFile}, it does not represent a persisted history entity.
 *
 * @param path source path inspected during import.
 * @param fileName display name resolved for the source.
 * @param sizeBytes source size in bytes at inspection time.
 * @param exists whether the source exists.
 * @param readable whether the source can be read.
 * @param regularFile whether the source is a regular file.
 * @param sourceKind origin of the JSON content.
 * @param documentFormat classified format for the imported source.
 */
public record JsonImportResult(
    Path path,
    String fileName,
    long sizeBytes,
    boolean exists,
    boolean readable,
    boolean regularFile,
    JsonDocumentSourceKind sourceKind,
    DocumentFormat documentFormat) {

  public JsonImportResult(
      Path path,
      String fileName,
      long sizeBytes,
      boolean exists,
      boolean readable,
      boolean regularFile,
      JsonDocumentSourceKind sourceKind) {
    this(path, fileName, sizeBytes, exists, readable, regularFile, sourceKind, DocumentFormat.JSON);
  }

  /** Validates mandatory fields for the import result state. */
  public JsonImportResult {
    if (sourceKind == null) {
      throw new IllegalArgumentException("Import source kind is required.");
    }
    if (documentFormat == null) {
      documentFormat = DocumentFormat.JSON;
    }
  }

  /**
   * Indicates whether the source is currently usable for further JSON processing.
   *
   * @return {@code true} when the source exists, is readable and is a regular file.
   */
  public boolean available() {
    return exists && readable && regularFile;
  }
}
