package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;

public interface JsonHistoryRepository {

    List<ImportedJsonFile> findAll();

    void save(ImportedJsonFile importedJsonFile);

    void deleteByStoredName(String storedName);
}
