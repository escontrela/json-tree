package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Output port for managing persisted JSON import history and related file content. */
public interface JsonHistoryRepository {

  /**
   * Returns all imported JSON entries stored in history.
   *
   * @return all persisted history entries.
   */
  List<ImportedJsonFile> findAll();

  /**
   * Finds a history entry by its stored file name.
   *
   * @param storedName persisted file name.
   * @return the matching history entry when it exists.
   */
  Optional<ImportedJsonFile> findByStoredName(String storedName);

  /**
   * Resolves the physical path of a stored JSON file.
   *
   * @param storedName persisted file name.
   * @return the resolved path when the stored file exists.
   */
  Optional<Path> resolveStoredJsonPath(String storedName);

  /**
   * Reads the raw JSON content of a stored history file.
   *
   * @param storedName persisted file name.
   * @return the JSON content when the file can be found and read.
   */
  Optional<String> readStoredJson(String storedName);

  /**
   * Persists a history entry and its JSON payload.
   *
   * @param importedJsonFile metadata for the imported file.
   * @param jsonContent JSON payload to persist.
   */
  void save(ImportedJsonFile importedJsonFile, String jsonContent);

  /**
   * Updates the favorite flag of a history entry.
   *
   * @param storedName persisted file name.
   * @param favorite new favorite state.
   * @return the updated history entry when it exists.
   */
  Optional<ImportedJsonFile> updateFavorite(String storedName, boolean favorite);

  /**
   * Deletes a history entry and its stored JSON file by file name.
   *
   * @param storedName persisted file name.
   */
  void deleteByStoredName(String storedName);
}
