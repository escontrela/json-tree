package com.davidpe.jsontree.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.infrastructure.config.AppDataProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemJsonHistoryRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void savesSnapshotsAndListsThemChronologically() {
        FileSystemJsonHistoryRepository repository = new FileSystemJsonHistoryRepository(properties(), new ObjectMapper().findAndRegisterModules());

        ImportedJsonFile newer = new ImportedJsonFile("2026-05-27_00-10-00_sample.json", "sample.json", Instant.parse("2026-05-27T00:10:00Z"), 20L, 4, true, false);
        ImportedJsonFile older = new ImportedJsonFile("2026-05-27_00-09-59_other.json", "other.json", Instant.parse("2026-05-27T00:09:59Z"), 10L, 2, true, true);

        repository.save(newer, "{\"b\":2}");
        repository.save(older, "{\"a\":1}");

        List<ImportedJsonFile> entries = repository.findAll();

        assertEquals(List.of(older, newer), entries);
        assertEquals("{\"a\":1}", repository.readStoredJson(older.storedName()).orElseThrow());
        assertTrue(tempDir.resolve("history").resolve(newer.storedName()).toFile().exists());
    }

    @Test
    void deletesSnapshotsAndMetadataEntries() {
        FileSystemJsonHistoryRepository repository = new FileSystemJsonHistoryRepository(properties(), new ObjectMapper().findAndRegisterModules());
        ImportedJsonFile entry = new ImportedJsonFile("2026-05-27_00-10-00_sample.json", "sample.json", Instant.parse("2026-05-27T00:10:00Z"), 20L, 4, true, true);

        repository.save(entry, "{\"b\":2}");
        repository.deleteByStoredName(entry.storedName());

        assertTrue(repository.findAll().isEmpty());
        assertFalse(repository.readStoredJson(entry.storedName()).isPresent());
    }

    @Test
    void updatesFavoriteStateWithoutBreakingSnapshotStorage() {
        FileSystemJsonHistoryRepository repository = new FileSystemJsonHistoryRepository(properties(), new ObjectMapper().findAndRegisterModules());
        ImportedJsonFile entry = new ImportedJsonFile("2026-05-27_00-10-00_sample.json", "sample.json", Instant.parse("2026-05-27T00:10:00Z"), 20L, 4, true, false);

        repository.save(entry, "{\"b\":2}");
        ImportedJsonFile updated = repository.updateFavorite(entry.storedName(), true).orElseThrow();

        assertTrue(updated.favorite());
        assertTrue(repository.findByStoredName(entry.storedName()).orElseThrow().favorite());
        assertEquals("{\"b\":2}", repository.readStoredJson(entry.storedName()).orElseThrow());
    }

    @Test
    void loadsExistingMetadataThatPredatesFavoriteField() throws Exception {
        FileSystemJsonHistoryRepository repository = new FileSystemJsonHistoryRepository(properties(), new ObjectMapper().findAndRegisterModules());
        java.nio.file.Files.createDirectories(tempDir);
        java.nio.file.Files.writeString(
                tempDir.resolve("metadata.json"),
                """
                [ {
                  "storedName" : "2026-05-27_00-10-00_sample.json",
                  "originalName" : "sample.json",
                  "importedAt" : 1779840600.000000000,
                  "sizeBytes" : 20,
                  "lineCount" : 4,
                  "valid" : true
                } ]
                """
        );

        List<ImportedJsonFile> entries = repository.findAll();

        assertEquals(1, entries.size());
        assertFalse(entries.getFirst().favorite());
    }

    private AppDataProperties properties() {
        AppDataProperties properties = new AppDataProperties();
        properties.setRootDirectory(tempDir);
        return properties;
    }
}
