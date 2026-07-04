package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.HistoryJsonImportResult;
import com.davidpe.jsontree.application.model.HistoryJsonImportStatus;
import com.davidpe.jsontree.application.port.out.JsonFileChooserPort;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.infrastructure.rendering.JacksonAsciiTreeFormatter;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.davidpe.jsontree.infrastructure.validation.JacksonJsonValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoryJsonImportServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void importsSelectedJsonIntoHistoryWithoutReplacingCurrentViewerSelection() throws IOException {
    Path validJson = tempDir.resolve("sample.json");
    Files.writeString(validJson, "{\"name\":\"json-tree\"}");
    InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
    JsonViewerWorkflowService workflowService = viewerWorkflowService(historyRepository);
    HistoryJsonImportService service =
        new HistoryJsonImportService(() -> Optional.of(validJson), workflowService);

    HistoryJsonImportResult result = service.importFromDisk();

    assertTrue(result.successful());
    assertEquals(HistoryJsonImportStatus.IMPORTED, result.status());
    assertEquals("sample.json", result.importedEntry().originalName());
    assertEquals(1, historyRepository.findAll().size());
    assertTrue(workflowService.currentView().isEmpty());
  }

  @Test
  void reportsCancelledWhenUserDismissesTheFilePicker() {
    HistoryJsonImportResult result =
        new HistoryJsonImportService(Optional::<Path>empty, viewerWorkflowService(new InMemoryHistoryRepository()))
            .importFromDisk();

    assertFalse(result.successful());
    assertEquals(HistoryJsonImportStatus.CANCELLED, result.status());
  }

  @Test
  void reportsUnreadableFileWhenSelectionDoesNotExist() {
    Path missingJson = tempDir.resolve("missing.json");
    HistoryJsonImportResult result =
        new HistoryJsonImportService(
                () -> Optional.of(missingJson),
                viewerWorkflowService(new InMemoryHistoryRepository()))
            .importFromDisk();

    assertFalse(result.successful());
    assertEquals(HistoryJsonImportStatus.UNREADABLE_FILE, result.status());
  }

  @Test
  void rejectsInvalidJsonWithoutPersistingHistoryEntry() throws IOException {
    Path invalidJson = tempDir.resolve("broken.json");
    Files.writeString(invalidJson, "{broken");
    InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
    HistoryJsonImportResult result =
        new HistoryJsonImportService(
                () -> Optional.of(invalidJson),
                viewerWorkflowService(historyRepository))
            .importFromDisk();

    assertFalse(result.successful());
    assertEquals(HistoryJsonImportStatus.INVALID_JSON, result.status());
    assertTrue(historyRepository.findAll().isEmpty());
  }

  @Test
  void reportsEmptyJsonWhenSelectedFileContainsNoContent() throws IOException {
    Path emptyJson = tempDir.resolve("empty.json");
    Files.writeString(emptyJson, "   ");
    HistoryJsonImportResult result =
        new HistoryJsonImportService(
                () -> Optional.of(emptyJson),
                viewerWorkflowService(new InMemoryHistoryRepository()))
            .importFromDisk();

    assertFalse(result.successful());
    assertEquals(HistoryJsonImportStatus.EMPTY_JSON, result.status());
  }

  private JsonViewerWorkflowService viewerWorkflowService(InMemoryHistoryRepository historyRepository) {
    ObjectMapper objectMapper = new ObjectMapper();
    return new JsonViewerWorkflowService(
        new JacksonJsonValidationService(objectMapper),
        historyRepository,
        new JacksonAsciiTreeFormatter(objectMapper),
        inspectionModeResolver(),
        fixedClock());
  }

  private JsonInspectionModeResolver inspectionModeResolver() {
    return new JsonInspectionModeResolver(new LargePreviewProperties());
  }

  private Clock fixedClock() {
    return Clock.fixed(Instant.parse("2026-07-04T09:30:00Z"), ZoneOffset.UTC);
  }

  private static final class InMemoryHistoryRepository
      implements com.davidpe.jsontree.application.port.out.JsonHistoryRepository {

    private final List<ImportedJsonFile> entries = new ArrayList<>();

    @Override
    public List<ImportedJsonFile> findAll() {
      return entries.stream().sorted(Comparator.comparing(ImportedJsonFile::importedAt)).toList();
    }

    @Override
    public Optional<ImportedJsonFile> findByStoredName(String storedName) {
      return entries.stream().filter(entry -> entry.storedName().equals(storedName)).findFirst();
    }

    @Override
    public Optional<String> readStoredJson(String storedName) {
      return Optional.empty();
    }

    @Override
    public void save(ImportedJsonFile importedJsonFile, String jsonContent) {
      entries.removeIf(existing -> existing.storedName().equals(importedJsonFile.storedName()));
      entries.add(importedJsonFile);
    }

    @Override
    public Optional<ImportedJsonFile> updateFavorite(String storedName, boolean favorite) {
      return Optional.empty();
    }

    @Override
    public void deleteByStoredName(String storedName) {
      entries.removeIf(existing -> existing.storedName().equals(storedName));
    }
  }
}
