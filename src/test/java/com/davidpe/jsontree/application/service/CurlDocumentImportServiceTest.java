package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.CurlCommandSource;
import com.davidpe.jsontree.application.model.CurlDocumentImportResult;
import com.davidpe.jsontree.application.model.CurlDocumentImportStatus;
import com.davidpe.jsontree.application.model.CurlExecutionRequest;
import com.davidpe.jsontree.application.model.CurlExecutionResult;
import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.application.port.out.CurlRequestExecutorPort;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.application.port.out.LargePreviewSessionStorePort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.DocumentFormat;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CurlDocumentImportServiceTest {

  @TempDir Path tempDir;

  @Test
  void materializesJsonResponseIntoViewerWorkflowAndHistory() {
    RecordingHistoryRepository repository = new RecordingHistoryRepository();
    CurlDocumentImportService service =
        new CurlDocumentImportService(
            request ->
                CurlExecutionResult.success(
                    200,
                    request.url(),
                    Map.of("Content-Type", List.of("application/json; charset=UTF-8")),
                    "{\"id\":1}".getBytes(StandardCharsets.UTF_8),
                    "application/json",
                    "UTF-8"),
            viewerWorkflowService(repository),
            fixedClock(),
            tempDir);

    CurlDocumentImportResult result = service.importRequest(sampleRequest());

    assertTrue(result.successful());
    assertNotNull(result.loadResult());
    assertEquals(DocumentFormat.JSON, result.loadResult().importResult().documentFormat());
    assertTrue(result.loadResult().importResult().fileName().endsWith(".json"));
    assertTrue(repository.lastSaved.curlBacked());
    assertEquals(sampleRequest().rawCommand(), repository.lastSaved.curlCommand());
  }

  @Test
  void materializesMarkdownResponseIntoViewerWorkflow() {
    CurlDocumentImportService service =
        new CurlDocumentImportService(
            request ->
                CurlExecutionResult.success(
                    200,
                    request.url(),
                    Map.of("Content-Type", List.of("text/markdown")),
                    "# Heading\n\ncontent".getBytes(StandardCharsets.UTF_8),
                    "text/markdown",
                    "UTF-8"),
            viewerWorkflowService(new RecordingHistoryRepository()),
            fixedClock(),
            tempDir);

    CurlDocumentImportResult result = service.importRequest(sampleRequest());

    assertTrue(result.successful());
    assertEquals(DocumentFormat.MARKDOWN, result.loadResult().importResult().documentFormat());
    assertTrue(result.loadResult().markdownDocument());
  }

  @Test
  void rejectsUnsupportedResponses() {
    CurlDocumentImportService service =
        new CurlDocumentImportService(
            request ->
                CurlExecutionResult.success(
                    200,
                    request.url(),
                    Map.of("Content-Type", List.of("text/plain")),
                    "hello world".getBytes(StandardCharsets.UTF_8),
                    "text/plain",
                    "UTF-8"),
            viewerWorkflowService(new RecordingHistoryRepository()),
            fixedClock(),
            tempDir);

    CurlDocumentImportResult result = service.importRequest(sampleRequest());

    assertFalse(result.successful());
    assertEquals(CurlDocumentImportStatus.UNSUPPORTED_RESPONSE, result.status());
    assertEquals(200, result.httpStatusCode());
    assertTrue(result.message().contains("HTTP 200"));
  }

  @Test
  void reportsAccessDeniedWithFriendlyHttpMessage() {
    CurlDocumentImportService service =
        new CurlDocumentImportService(
            request ->
                CurlExecutionResult.success(
                    403,
                    request.url(),
                    Map.of("Content-Type", List.of("application/json")),
                    "{\"message\":\"forbidden\"}".getBytes(StandardCharsets.UTF_8),
                    "application/json",
                    "UTF-8"),
            viewerWorkflowService(new RecordingHistoryRepository()),
            fixedClock(),
            tempDir);

    CurlDocumentImportResult result = service.importRequest(sampleRequest());

    assertFalse(result.successful());
    assertEquals(CurlDocumentImportStatus.EXECUTION_FAILED, result.status());
    assertEquals(403, result.httpStatusCode());
    assertEquals("HTTP 403: access denied by the remote endpoint.", result.message());
  }

  private CurlExecutionRequest sampleRequest() {
    return new CurlExecutionRequest(
        "curl https://example.com/items",
        CurlCommandSource.clipboard(),
        URI.create("https://example.com/items"),
        "GET",
        false,
        List.of(),
        "");
  }

  private JsonViewerWorkflowService viewerWorkflowService(RecordingHistoryRepository repository) {
    JsonValidationPort validationPort =
        path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
    AsciiTreeRendererPort rendererPort =
        path -> new AsciiTreeDocument("root", "root\n└─ id: 1", 2);
    return new JsonViewerWorkflowService(
        validationPort,
        repository,
        rendererPort,
        new JsonInspectionModeResolver(new LargePreviewProperties()),
        new LargePreviewSessionService(new NoOpLargePreviewSessionStore(), 2, new DirectExecutorService()),
        fixedClock());
  }

  private Clock fixedClock() {
    return Clock.fixed(Instant.parse("2026-07-19T10:15:30Z"), ZoneOffset.UTC);
  }

  private static final class RecordingHistoryRepository implements JsonHistoryRepository {

    private ImportedJsonFile lastSaved;

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
    public void save(ImportedJsonFile importedJsonFile, String jsonContent) {
      this.lastSaved = importedJsonFile;
    }

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
