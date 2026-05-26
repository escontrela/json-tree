package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;
import java.util.Optional;

public interface JsonHistoryRepository {

    List<ImportedJsonFile> findAll();

    Optional<ImportedJsonFile> findByStoredName(String storedName);

    Optional<String> readStoredJson(String storedName);

    void save(ImportedJsonFile importedJsonFile, String jsonContent);

    void deleteByStoredName(String storedName);
}
