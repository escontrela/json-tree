package com.davidpe.jsontree.domain.model;

import java.time.Instant;

/**
 * Domain entity representing a JSON file already registered in system history.
 *
 * <p>This record models persisted identity and attributes (stored name, original name, timestamps
 * and flags) after import is completed. Unlike {@link JsonImportResult}, it is not a process-state
 * snapshot of the read operation.
 *
 * @param storedName persisted file name used by local storage.
 * @param originalName original file name from the source.
 * @param importedAt timestamp when the file was imported into history.
 * @param sizeBytes persisted file size in bytes.
 * @param lineCount number of lines detected for the stored content.
 * @param valid whether the stored JSON content is valid.
 * @param favorite whether the history entry is marked as favorite.
 * @param documentFormat classified format for the persisted entry.
 */
public record ImportedJsonFile(
    String storedName,
    String originalName,
    Instant importedAt,
    long sizeBytes,
    int lineCount,
    boolean valid,
    boolean favorite,
    DocumentFormat documentFormat) {

  public ImportedJsonFile(
      String storedName,
      String originalName,
      Instant importedAt,
      long sizeBytes,
      int lineCount,
      boolean valid,
      boolean favorite) {
    this(
        storedName,
        originalName,
        importedAt,
        sizeBytes,
        lineCount,
        valid,
        favorite,
        DocumentFormat.JSON);
  }

  public ImportedJsonFile {
    if (documentFormat == null) {
      documentFormat = DocumentFormat.JSON;
    }
  }

  /**
   * Creates a new immutable instance with an updated favorite flag.
   *
   * @param nextFavorite desired favorite state.
   * @return a copy of this entity with the provided favorite value.
   */
  public ImportedJsonFile withFavorite(boolean nextFavorite) {
    return new ImportedJsonFile(
        storedName,
        originalName,
        importedAt,
        sizeBytes,
        lineCount,
        valid,
        nextFavorite,
        documentFormat);
  }
}
