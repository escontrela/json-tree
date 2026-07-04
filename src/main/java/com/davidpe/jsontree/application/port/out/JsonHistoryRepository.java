package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface JsonHistoryRepository {

    List<ImportedJsonFile> findAll();

    Optional<ImportedJsonFile> findByStoredName(String storedName);

    Optional<Path> resolveStoredJsonPath(String storedName);

    Optional<String> readStoredJson(String storedName);

    void save(ImportedJsonFile importedJsonFile, String jsonContent);

    Optional<ImportedJsonFile> updateFavorite(String storedName, boolean favorite);

    void deleteByStoredName(String storedName);
}
