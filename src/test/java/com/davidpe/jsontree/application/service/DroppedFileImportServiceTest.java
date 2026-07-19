package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.CurlDocumentImportResult;
import com.davidpe.jsontree.application.model.CurlDocumentImportStatus;
import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.application.port.out.LargePreviewSessionStorePort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DroppedFileImportServiceTest {

  @TempDir Path tempDir;

  @Test
  void preservesNativeJsonDropBehavior() throws Exception {
    Path jsonPath = Files.writeString(tempDir.resolve("sample.json"), "{\"id\":1}");
    DroppedFileImportService service =
        new DroppedFileImportService(
            viewerWorkflowService(),
            new CurlCommandParserService(),
            fakeCurlService(
                CurlDocumentImportResult.failure(
                    CurlDocumentImportStatus.EXECUTION_FAILED, "should not run")));

    var result = service.importDroppedFile(jsonPath);

    assertTrue(result.successful());
    assertEquals(JsonDocumentSourceKind.LOCAL_FILE, result.loadResult().importResult().sourceKind());
  }

  @Test
  void routesDroppedCurlFileIntoCurlWorkflow() throws Exception {
    Path curlFile =
        Files.writeString(
            tempDir.resolve("request.txt"), "curl https://example.com/items --header 'X-Test: demo'");
    DroppedFileImportService service =
        new DroppedFileImportService(
            viewerWorkflowService(),
            new CurlCommandParserService(),
            fakeCurlService(
                CurlDocumentImportResult.failure(
                    CurlDocumentImportStatus.EXECUTION_FAILED, "network down")));

    var result = service.importDroppedFile(curlFile);

    assertEquals("network down", result.message());
    assertEquals(
        com.davidpe.jsontree.application.model.DroppedFileImportStatus.EXECUTION_FAILED,
        result.status());
  }

  @Test
  void rejectsNonDocumentNonCurlFiles() throws Exception {
    Path textFile = Files.writeString(tempDir.resolve("notes.txt"), "hello world");
    DroppedFileImportService service =
        new DroppedFileImportService(
            viewerWorkflowService(),
            new CurlCommandParserService(),
            fakeCurlService(
                CurlDocumentImportResult.failure(
                    CurlDocumentImportStatus.EXECUTION_FAILED, "unused")));

    var result = service.importDroppedFile(textFile);

    assertEquals(
        com.davidpe.jsontree.application.model.DroppedFileImportStatus.UNSUPPORTED_DROP,
        result.status());
  }

  private JsonViewerWorkflowService viewerWorkflowService() {
    return new JsonViewerWorkflowService(
        path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
        new InMemoryHistoryRepository(),
        path -> new AsciiTreeDocument("root", "root", 1),
        new JsonInspectionModeResolver(new LargePreviewProperties()),
        new LargePreviewSessionService(new NoOpLargePreviewSessionStore(), 2, new DirectExecutorService()),
        Clock.fixed(Instant.parse("2026-07-19T10:15:30Z"), ZoneOffset.UTC));
  }

  private CurlDocumentImportService fakeCurlService(CurlDocumentImportResult response) {
    return new CurlDocumentImportService(
        request -> {
          throw new AssertionError("Base executor should not be called in fake test service.");
        },
        viewerWorkflowService(),
        Clock.fixed(Instant.parse("2026-07-19T10:15:30Z"), ZoneOffset.UTC),
        tempDir) {
      @Override
      public CurlDocumentImportResult importRequest(
          com.davidpe.jsontree.application.model.CurlExecutionRequest request) {
        return response;
      }
    };
  }

  private static final class InMemoryHistoryRepository implements JsonHistoryRepository {

    @Override
    public List<ImportedJsonFile> findAll() {
      return List.of();
    }

    @Override
    public Optional<ImportedJsonFile> findByStoredName(String storedName) {
      return Optional.empty();
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
    public void save(ImportedJsonFile importedJsonFile, String jsonContent) {}

    @Override
    public Optional<ImportedJsonFile> updateFavorite(String storedName, boolean favorite) {
      return Optional.empty();
    }

    @Override
    public void deleteByStoredName(String storedName) {}
  }

  private static final class NoOpLargePreviewSessionStore implements LargePreviewSessionStorePort {

    @Override
    public LargePreviewMaterializationSnapshot materialize(
        String sessionId,
        LargePreviewSessionSource source,
        java.util.function.Consumer<LargePreviewPageDescriptor> onPageAvailable) {
      throw new IllegalStateException("Large preview should not be used in this test.");
    }

    @Override
    public Optional<LargePreviewPageContent> readPage(LargePreviewPageDescriptor descriptor) {
      return Optional.empty();
    }

    @Override
    public void deleteSessionStorage(Path sessionStoragePath) {}
  }

  private static final class DirectExecutorService extends AbstractExecutorService {

    @Override
    public void shutdown() {}

    @Override
    public List<Runnable> shutdownNow() {
      return List.of();
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return true;
    }

    @Override
    public void execute(Runnable command) {
      command.run();
    }
  }
}
