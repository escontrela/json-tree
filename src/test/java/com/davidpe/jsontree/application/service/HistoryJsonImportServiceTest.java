package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.HistoryJsonImportResult;
import com.davidpe.jsontree.application.model.HistoryJsonImportStatus;
import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.application.port.out.LargePreviewSessionStorePort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.davidpe.jsontree.infrastructure.rendering.JacksonAsciiTreeFormatter;
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
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoryJsonImportServiceTest {

  @TempDir Path tempDir;

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
        new HistoryJsonImportService(
                Optional::<Path>empty, viewerWorkflowService(new InMemoryHistoryRepository()))
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
                () -> Optional.of(invalidJson), viewerWorkflowService(historyRepository))
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

  @Test
  void importsOversizedJsonIntoHistoryThroughLargePreviewRendererPath() throws IOException {
    Path largeJson = tempDir.resolve("oversized.json");
    Files.writeString(largeJson, "{\"payload\":\"" + "0123456789".repeat(140) + "\"}");
    InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
    TrackingRendererPort rendererPort = new TrackingRendererPort();
    JsonViewerWorkflowService workflowService =
        viewerWorkflowService(historyRepository, rendererPort, 8L);

    HistoryJsonImportResult result =
        new HistoryJsonImportService(() -> Optional.of(largeJson), workflowService)
            .importFromDisk();

    assertTrue(result.successful());
    assertTrue(rendererPort.largePreviewUsed);
  }

  private JsonViewerWorkflowService viewerWorkflowService(
      InMemoryHistoryRepository historyRepository) {
    return viewerWorkflowService(
        historyRepository, new JacksonAsciiTreeFormatter(new ObjectMapper()), Long.MAX_VALUE);
  }

  private JsonViewerWorkflowService viewerWorkflowService(
      InMemoryHistoryRepository historyRepository,
      AsciiTreeRendererPort rendererPort,
      long fullRenderMaxBytes) {
    ObjectMapper objectMapper = new ObjectMapper();
    LargePreviewProperties properties = new LargePreviewProperties();
    properties.setFullRenderMaxBytes(fullRenderMaxBytes);
    return new JsonViewerWorkflowService(
        new JacksonJsonValidationService(objectMapper, properties),
        historyRepository,
        rendererPort,
        new JsonInspectionModeResolver(properties),
        new LargePreviewSessionService(
            new NoOpLargePreviewSessionStore(), 2, new DirectExecutorService()),
        fixedClock());
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
    public Optional<Path> resolveStoredJsonPath(String storedName) {
      return Optional.empty();
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

  private static final class TrackingRendererPort implements AsciiTreeRendererPort {

    private boolean largePreviewUsed;

    @Override
    public AsciiTreeDocument render(Path jsonFilePath) {
      return new AsciiTreeDocument("root", "root\n├─ full: true", 2);
    }

    @Override
    public AsciiTreeDocument renderLargePreview(Path jsonFilePath) {
      largePreviewUsed = true;
      return new AsciiTreeDocument("root", "root\n├─ preview: true", 2);
    }
  }

  private static final class NoOpLargePreviewSessionStore implements LargePreviewSessionStorePort {

    @Override
    public LargePreviewMaterializationSnapshot materialize(
        String sessionId,
        LargePreviewSessionSource source,
        java.util.function.Consumer<LargePreviewPageDescriptor> onPageAvailable) {
      throw new IllegalStateException(
          "Large preview sessions are not part of this history import test.");
    }

    @Override
    public Optional<LargePreviewPageContent> readPage(LargePreviewPageDescriptor descriptor) {
      return Optional.empty();
    }

    @Override
    public void deleteSessionStorage(Path sessionStoragePath) {}
  }

  private static final class DirectExecutorService extends AbstractExecutorService {

    private boolean shutdown;

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdown = true;
      return List.of();
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    @Override
    public boolean isTerminated() {
      return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return shutdown;
    }

    @Override
    public void execute(Runnable command) {
      command.run();
    }
  }
}
