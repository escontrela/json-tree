package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.HistoryJsonSearchResult;
import com.davidpe.jsontree.application.model.HistoryJsonSearchStatus;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;

class HistoryJsonSearchServiceTest {

  @Test
  void returnsMatchingHistoryEntriesForAStoredJsonQuery() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile matching = entry("matching", Instant.parse("2026-07-04T10:00:00Z"), false);
    ImportedJsonFile other = entry("other", Instant.parse("2026-07-04T11:00:00Z"), true);
    repository.save(matching, "{\"name\":\"David\",\"role\":\"admin\"}");
    repository.save(other, "{\"name\":\"Alice\",\"role\":\"viewer\"}");

    HistoryJsonSearchResult result = new HistoryJsonSearchService(repository).search("admin", true);

    assertEquals(HistoryJsonSearchStatus.MATCHES, result.status());
    assertEquals("admin", result.query());
    assertEquals(List.of(matching), result.entries());
  }

  @Test
  void returnsMatchingHistoryEntriesForAnOriginalFileNameQuery() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile matching =
        new ImportedJsonFile(
            "2026-07-04_10-00-00_api-response.json",
            "api-response.json",
            Instant.parse("2026-07-04T10:00:00Z"),
            20L,
            4,
            true,
            false);
    ImportedJsonFile other = entry("other", Instant.parse("2026-07-04T11:00:00Z"), true);
    repository.save(matching, "{\"name\":\"David\"}");
    repository.save(other, "{\"name\":\"Alice\"}");

    HistoryJsonSearchResult result =
        new HistoryJsonSearchService(repository).search("response", true);

    assertEquals(HistoryJsonSearchStatus.MATCHES, result.status());
    assertEquals(List.of(matching), result.entries());
  }

  @Test
  void returnsMatchingHistoryEntriesForAStoredSnapshotNameQuery() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile matching =
        new ImportedJsonFile(
            "2026-07-04_10-00-00_customer-export.json",
            "export.json",
            Instant.parse("2026-07-04T10:00:00Z"),
            20L,
            4,
            true,
            false);
    ImportedJsonFile other = entry("other", Instant.parse("2026-07-04T11:00:00Z"), true);
    repository.save(matching, "{\"name\":\"David\"}");
    repository.save(other, "{\"name\":\"Alice\"}");

    HistoryJsonSearchResult result =
        new HistoryJsonSearchService(repository).search("customer-export", true);

    assertEquals(HistoryJsonSearchStatus.MATCHES, result.status());
    assertEquals(List.of(matching), result.entries());
  }

  @Test
  void blankQueryClearsTheHistorySearchBackToFullArchive() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile first = entry("first", Instant.parse("2026-07-04T10:00:00Z"), false);
    ImportedJsonFile second = entry("second", Instant.parse("2026-07-04T11:00:00Z"), true);
    repository.save(first, "{\"id\":1}");
    repository.save(second, "{\"id\":2}");

    HistoryJsonSearchResult result = new HistoryJsonSearchService(repository).search("   ", true);

    assertEquals(HistoryJsonSearchStatus.CLEARED, result.status());
    assertEquals(List.of(first, second), result.entries());
    assertFalse(result.searchActive());
  }

  @Test
  void returnsNoResultsWhenNoStoredJsonMatchesTheQuery() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    repository.save(entry("sample", Instant.parse("2026-07-04T10:00:00Z"), false), "{\"id\":1}");

    HistoryJsonSearchResult result =
        new HistoryJsonSearchService(repository).search("missing", true);

    assertEquals(HistoryJsonSearchStatus.NO_RESULTS, result.status());
    assertTrue(result.entries().isEmpty());
    assertEquals("missing", result.query());
  }

  @Test
  void blocksSearchExecutionWhenFavoritesOnlyModeOwnsTheScreen() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile entry = entry("sample", Instant.parse("2026-07-04T10:00:00Z"), true);
    repository.save(entry, "{\"id\":1}");

    HistoryJsonSearchResult result = new HistoryJsonSearchService(repository).search("id", false);

    assertEquals(HistoryJsonSearchStatus.BLOCKED, result.status());
    assertEquals(List.of(entry), result.entries());
    assertFalse(result.searchActive());
  }

  @Test
  void rerunningSearchAfterDeletingAFilteredEntryRefreshesTheResultSet() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile entry = entry("sample", Instant.parse("2026-07-04T10:00:00Z"), false);
    repository.save(entry, "{\"tag\":\"match-me\"}");
    HistoryJsonSearchService service = new HistoryJsonSearchService(repository);

    HistoryJsonSearchResult firstSearch = service.search("match-me", true);
    repository.deleteByStoredName(entry.storedName());
    HistoryJsonSearchResult secondSearch = service.search("match-me", true);

    assertEquals(HistoryJsonSearchStatus.MATCHES, firstSearch.status());
    assertEquals(List.of(entry), firstSearch.entries());
    assertEquals(HistoryJsonSearchStatus.NO_RESULTS, secondSearch.status());
    assertTrue(secondSearch.entries().isEmpty());
  }

  private ImportedJsonFile entry(String name, Instant importedAt, boolean favorite) {
    return new ImportedJsonFile(
        "2026-07-04_10-00-00_" + name + ".json",
        name + ".json",
        importedAt,
        20L,
        4,
        true,
        favorite);
  }

  private static final class InMemoryHistoryRepository implements JsonHistoryRepository {

    private final ConcurrentMap<String, ImportedJsonFile> entries = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> storedJson = new ConcurrentHashMap<>();

    @Override
    public List<ImportedJsonFile> findAll() {
      return entries.values().stream().sorted(Comparator.comparing(ImportedJsonFile::importedAt)).toList();
    }

    @Override
    public Optional<ImportedJsonFile> findByStoredName(String storedName) {
      return Optional.ofNullable(entries.get(storedName));
    }

    @Override
    public Optional<Path> resolveStoredJsonPath(String storedName) {
      return Optional.empty();
    }

    @Override
    public Optional<String> readStoredJson(String storedName) {
      return Optional.ofNullable(storedJson.get(storedName));
    }

    @Override
    public void save(ImportedJsonFile importedJsonFile, String jsonContent) {
      entries.put(importedJsonFile.storedName(), importedJsonFile);
      storedJson.put(importedJsonFile.storedName(), jsonContent);
    }

    @Override
    public Optional<ImportedJsonFile> updateFavorite(String storedName, boolean favorite) {
      ImportedJsonFile existing = entries.get(storedName);
      if (existing == null) {
        return Optional.empty();
      }
      ImportedJsonFile updated = existing.withFavorite(favorite);
      entries.put(storedName, updated);
      return Optional.of(updated);
    }

    @Override
    public void deleteByStoredName(String storedName) {
      entries.remove(storedName);
      storedJson.remove(storedName);
    }
  }
}
