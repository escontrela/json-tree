package com.davidpe.jsontree.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.DocumentFormat;
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
    void deletingFavoriteEntryLeavesRemainingHistoryConsistent() {
        FileSystemJsonHistoryRepository repository = new FileSystemJsonHistoryRepository(properties(), new ObjectMapper().findAndRegisterModules());
        ImportedJsonFile favorite = new ImportedJsonFile("2026-05-27_00-09-59_favorite.json", "favorite.json", Instant.parse("2026-05-27T00:09:59Z"), 10L, 2, true, true);
        ImportedJsonFile regular = new ImportedJsonFile("2026-05-27_00-10-00_regular.json", "regular.json", Instant.parse("2026-05-27T00:10:00Z"), 20L, 4, true, false);

        repository.save(favorite, "{\"favorite\":true}");
        repository.save(regular, "{\"regular\":true}");
        repository.deleteByStoredName(favorite.storedName());

        assertEquals(List.of(regular), repository.findAll());
        assertFalse(repository.readStoredJson(favorite.storedName()).isPresent());
        assertFalse(repository.findByStoredName(favorite.storedName()).isPresent());
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
    void resolvesStoredSnapshotPathWithoutReadingWholeSnapshotContent() {
        FileSystemJsonHistoryRepository repository = new FileSystemJsonHistoryRepository(properties(), new ObjectMapper().findAndRegisterModules());
        ImportedJsonFile entry = new ImportedJsonFile("2026-05-27_00-10-00_sample.json", "sample.json", Instant.parse("2026-05-27T00:10:00Z"), 20L, 4, true, false);

        repository.save(entry, "{\"b\":2}");

        assertEquals(
                tempDir.resolve("history").resolve(entry.storedName()),
                repository.resolveStoredJsonPath(entry.storedName()).orElseThrow());
    }

    @Test
    void returnsEmptyWhenStoredSnapshotPathDoesNotExist() {
        FileSystemJsonHistoryRepository repository = new FileSystemJsonHistoryRepository(properties(), new ObjectMapper().findAndRegisterModules());

        assertTrue(repository.resolveStoredJsonPath("missing.json").isEmpty());
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
        assertEquals(DocumentFormat.JSON, entries.getFirst().documentFormat());
    }

    @Test
    void savesMarkdownSnapshotsWithFormatAwareMetadataAndExtension() {
        FileSystemJsonHistoryRepository repository =
                new FileSystemJsonHistoryRepository(properties(), new ObjectMapper().findAndRegisterModules());
        ImportedJsonFile entry = new ImportedJsonFile(
                "2026-07-19_12-00-00_notes.md",
                "notes.md",
                Instant.parse("2026-07-19T12:00:00Z"),
                512L,
                24,
                true,
                false,
                DocumentFormat.MARKDOWN);

        repository.save(entry, "# Heading\n\ncontent");

        ImportedJsonFile stored = repository.findByStoredName(entry.storedName()).orElseThrow();
        assertEquals(DocumentFormat.MARKDOWN, stored.documentFormat());
        assertTrue(stored.storedName().endsWith(".md"));
        assertEquals(
                tempDir.resolve("history").resolve(entry.storedName()),
                repository.resolveStoredJsonPath(entry.storedName()).orElseThrow());
        assertEquals("# Heading\n\ncontent", repository.readStoredJson(entry.storedName()).orElseThrow());
    }

    private AppDataProperties properties() {
        AppDataProperties properties = new AppDataProperties();
        properties.setRootDirectory(tempDir);
        return properties;
    }
}
