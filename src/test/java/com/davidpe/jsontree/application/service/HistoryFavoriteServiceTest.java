package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.HistoryFavoriteToggleStatus;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;

class HistoryFavoriteServiceTest {

  @Test
  void favoritesExistingHistoryEntry() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile entry = sampleEntry(false);
    repository.save(entry, "{\"id\":1}");

    HistoryFavoriteService service = new HistoryFavoriteService(repository);
    var result = service.toggleFavorite(entry.storedName());

    assertTrue(result.found());
    assertEquals(HistoryFavoriteToggleStatus.FAVORITED, result.status());
    assertTrue(repository.findByStoredName(entry.storedName()).orElseThrow().favorite());
  }

  @Test
  void unfavoritesExistingHistoryEntry() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile entry = sampleEntry(true);
    repository.save(entry, "{\"id\":1}");

    HistoryFavoriteService service = new HistoryFavoriteService(repository);
    var result = service.toggleFavorite(entry.storedName());

    assertTrue(result.found());
    assertEquals(HistoryFavoriteToggleStatus.UNFAVORITED, result.status());
    assertFalse(repository.findByStoredName(entry.storedName()).orElseThrow().favorite());
  }

  @Test
  void handlesMissingEntriesSafely() {
    HistoryFavoriteService service = new HistoryFavoriteService(new InMemoryHistoryRepository());

    var result = service.toggleFavorite("missing.json");

    assertEquals(HistoryFavoriteToggleStatus.MISSING, result.status());
    assertFalse(result.found());
  }

  @Test
  void repeatedTogglesFlipFavoriteStatePredictably() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    ImportedJsonFile entry = sampleEntry(false);
    repository.save(entry, "{\"id\":1}");

    HistoryFavoriteService service = new HistoryFavoriteService(repository);

    var first = service.toggleFavorite(entry.storedName());
    var second = service.toggleFavorite(entry.storedName());

    assertEquals(HistoryFavoriteToggleStatus.FAVORITED, first.status());
    assertEquals(HistoryFavoriteToggleStatus.UNFAVORITED, second.status());
    assertFalse(repository.findByStoredName(entry.storedName()).orElseThrow().favorite());
  }

  private ImportedJsonFile sampleEntry(boolean favorite) {
    return new ImportedJsonFile(
        "2026-07-03_00-10-00_sample.json",
        "sample.json",
        Instant.parse("2026-07-03T00:10:00Z"),
        24L,
        4,
        true,
        favorite);
  }

  private static final class InMemoryHistoryRepository implements JsonHistoryRepository {

    private final ConcurrentMap<String, ImportedJsonFile> entries = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> jsonContent = new ConcurrentHashMap<>();

    @Override
    public List<ImportedJsonFile> findAll() {
      return new ArrayList<>(entries.values());
    }

    @Override
    public Optional<ImportedJsonFile> findByStoredName(String storedName) {
      return Optional.ofNullable(entries.get(storedName));
    }

    @Override
    public Optional<String> readStoredJson(String storedName) {
      return Optional.ofNullable(jsonContent.get(storedName));
    }

    @Override
    public void save(ImportedJsonFile importedJsonFile, String json) {
      entries.put(importedJsonFile.storedName(), importedJsonFile);
      jsonContent.put(importedJsonFile.storedName(), json);
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
      jsonContent.remove(storedName);
    }
  }
}
