package com.davidpe.jsontree.infrastructure.persistence;

import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class FileSystemJsonHistoryRepository implements JsonHistoryRepository {

    @Override
    public List<ImportedJsonFile> findAll() {
        return new ArrayList<>();
    }

    @Override
    public void save(ImportedJsonFile importedJsonFile) {
        throw new UnsupportedOperationException("Pending implementation.");
    }

    @Override
    public void deleteByStoredName(String storedName) {
        throw new UnsupportedOperationException("Pending implementation.");
    }
}
