package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewOutlineDigest;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.application.port.out.LargePreviewSessionStorePort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonViewerWorkflowServiceTest {

  private final TrackingLargePreviewSessionStore initialLargePreviewStore =
      new TrackingLargePreviewSessionStore();
  private final JsonViewerWorkflowService service =
      new JsonViewerWorkflowService(
          unusedValidationPort(),
          new InMemoryHistoryRepository(),
          unusedRendererPort(),
          inspectionModeResolver(1024L),
          largePreviewSessionService(initialLargePreviewStore));

  @TempDir Path tempDir;

  @Test
  void importsAvailableFileMetadata() throws IOException {
    Path jsonFile = Files.writeString(tempDir.resolve("sample.json"), "{\n  \"name\": \"json-tree\"\n}");

    JsonImportResult result = service.importFile(jsonFile);

    assertNotNull(result.path());
    assertEquals(jsonFile.toAbsolutePath().normalize(), result.path());
    assertEquals("sample.json", result.fileName());
    assertEquals(Files.size(jsonFile), result.sizeBytes());
    assertTrue(result.exists());
    assertTrue(result.readable());
    assertTrue(result.regularFile());
    assertTrue(result.available());
  }

  @Test
  void reportsUnavailablePathWithoutThrowing() {
    Path missingFile = tempDir.resolve("missing.json");

    JsonImportResult result = service.importFile(missingFile);

    assertEquals(missingFile.toAbsolutePath().normalize(), result.path());
    assertEquals("missing.json", result.fileName());
    assertEquals(0L, result.sizeBytes());
    assertFalse(result.exists());
    assertFalse(result.available());
    assertFalse(result.regularFile());
  }

  @Test
  void loadsImportedFileUsingExistingImportResultObject() throws IOException {
    JsonViewerWorkflowService workflowService =
        new JsonViewerWorkflowService(
            path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
            new InMemoryHistoryRepository(),
            path -> new AsciiTreeDocument("root", "root\n└─ id: 1", 2),
            inspectionModeResolver(1024L),
            largePreviewSessionService(new TrackingLargePreviewSessionStore()));
    Path importedFile = Files.writeString(tempDir.resolve("imported.json"), "{\"id\":1}");

    JsonViewerLoadResult result =
        workflowService.loadImportedFile(
            new JsonImportResult(
                importedFile,
                "imported.json",
                16L,
                true,
                true,
                true,
                JsonDocumentSourceKind.LOCAL_FILE));

    assertTrue(result.validationResult().valid());
    assertTrue(result.hasRenderableTree());
    assertEquals("imported.json", result.importResult().fileName());
    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.FULL, result.inspectionMode());
    assertTrue(result.capabilities().rawJsonAvailable());
    assertTrue(result.capabilities().searchAvailable());
    assertFalse(result.hasLargePreviewSession());
  }

  @Test
  void classifiesLargeFilesBeforeOpeningPagedSession() throws IOException {
    AtomicBoolean classifiedBeforeOpen = new AtomicBoolean(false);
    JsonInspectionModeResolver resolver =
        new JsonInspectionModeResolver(properties(16L)) {
          @Override
          public com.davidpe.jsontree.application.model.JsonInspectionMode resolve(
              JsonImportResult importResult) {
            classifiedBeforeOpen.set(true);
            return super.resolve(importResult);
          }
        };
    TrackingLargePreviewSessionStore largePreviewStore = new TrackingLargePreviewSessionStore();
    JsonViewerWorkflowService workflowService =
        new JsonViewerWorkflowService(
            path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
            new InMemoryHistoryRepository(),
            path -> new AsciiTreeDocument("root", "root\n└─ id: 1", 2),
            resolver,
            largePreviewSessionService(
                largePreviewStore, source -> assertTrue(classifiedBeforeOpen.get())));
    Path importedFile =
        Files.writeString(
            tempDir.resolve("large.json"), "{\"id\":1,\"payload\":\"01234567890123456789\"}");

    JsonViewerLoadResult result =
        workflowService.loadImportedFile(
            new JsonImportResult(
                importedFile,
                "large.json",
                128L,
                true,
                true,
                true,
                JsonDocumentSourceKind.LOCAL_FILE));

    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.LARGE_PREVIEW,
        result.inspectionMode());
    assertTrue(result.capabilities().rawJsonAvailable());
    assertFalse(result.capabilities().searchAvailable());
    assertFalse(result.capabilities().outlineAvailable());
    assertTrue(result.hasLargePreviewSession());
    assertEquals(1, largePreviewStore.materializeCalls);
  }

  @Test
  void classifiesHistoryReopenUsingStoredMetadataSize() throws IOException {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    TrackingRendererPort renderer = new TrackingRendererPort();
    TrackingLargePreviewSessionStore largePreviewStore = new TrackingLargePreviewSessionStore();
    ImportedJsonFile historyEntry =
        new ImportedJsonFile(
            "2026-07-04_10-00-00_large.json",
            "large.json",
            Instant.parse("2026-07-04T10:00:00Z"),
            128L,
            3,
            true,
            false);
    repository.entry = historyEntry;
    repository.storedJson = "{\"id\":1}";
    repository.storedJsonPath =
        Files.writeString(tempDir.resolve(historyEntry.storedName()), repository.storedJson);
    JsonViewerWorkflowService workflowService =
        new JsonViewerWorkflowService(
            unusedValidationPort(),
            repository,
            renderer,
            inspectionModeResolver(16L),
            largePreviewSessionService(largePreviewStore));

    JsonViewerLoadResult result =
        workflowService.reopenHistoryEntry(historyEntry.storedName()).orElseThrow();

    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.LARGE_PREVIEW,
        result.inspectionMode());
    assertTrue(result.capabilities().rawJsonAvailable());
    assertFalse(result.capabilities().outlineAvailable());
    assertEquals(0, renderer.fullRenderCount);
    assertEquals(0, renderer.largePreviewRenderCount);
    assertTrue(result.hasLargePreviewSession());
    assertEquals(1, largePreviewStore.materializeCalls);
  }

  @Test
  void switchesRepeatedlyBetweenFullAndLargePreviewModes() throws IOException {
    TrackingRendererPort renderer = new TrackingRendererPort();
    TrackingLargePreviewSessionStore largePreviewStore = new TrackingLargePreviewSessionStore();
    JsonViewerWorkflowService workflowService =
        new JsonViewerWorkflowService(
            unusedValidationPort(),
            new InMemoryHistoryRepository(),
            renderer,
            inspectionModeResolver(32L),
            largePreviewSessionService(largePreviewStore));
    Path smallFile = Files.writeString(tempDir.resolve("small.json"), "{\"id\":1}");
    Path largeFile =
        Files.writeString(
            tempDir.resolve("large.json"),
            "{\"id\":1,\"payload\":\"0123456789012345678901234567890123456789\"}");

    JsonViewerLoadResult smallResult = workflowService.loadFile(smallFile);
    JsonViewerLoadResult largeResult = workflowService.loadFile(largeFile);
    JsonViewerLoadResult smallAgainResult = workflowService.loadFile(smallFile);

    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.FULL,
        smallResult.inspectionMode());
    assertTrue(smallResult.capabilities().rawJsonAvailable());
    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.LARGE_PREVIEW,
        largeResult.inspectionMode());
    assertFalse(largeResult.capabilities().searchAvailable());
    assertTrue(largeResult.hasLargePreviewSession());
    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.FULL,
        smallAgainResult.inspectionMode());
    assertTrue(smallAgainResult.capabilities().outlineAvailable());
    assertEquals(2, renderer.fullRenderCount);
    assertEquals(0, renderer.largePreviewRenderCount);
    assertEquals(1, largePreviewStore.materializeCalls);
    assertEquals(1, largePreviewStore.deletedSessionPaths.size());
    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.FULL,
        workflowService.currentView().orElseThrow().inspectionMode());
  }

  @Test
  void navigatesPagedLargePreviewForwardAndBackwardBeforeReturningToFullMode() throws IOException {
    TrackingRendererPort renderer = new TrackingRendererPort();
    TrackingLargePreviewSessionStore largePreviewStore =
        new TrackingLargePreviewSessionStore(
            List.of(
                "root\n├─ page: 0",
                "root\n├─ page: 1",
                "root\n├─ page: 2"),
            outlineDigestForPageIndexes(0, 1, 2));
    JsonViewerWorkflowService workflowService =
        new JsonViewerWorkflowService(
            unusedValidationPort(),
            new InMemoryHistoryRepository(),
            renderer,
            inspectionModeResolver(32L),
            largePreviewSessionService(largePreviewStore));
    Path largeFile =
        Files.writeString(
            tempDir.resolve("large.json"),
            "{\"id\":1,\"payload\":\"0123456789012345678901234567890123456789\"}");
    Path smallFile = Files.writeString(tempDir.resolve("small.json"), "{\"small\":true}");

    JsonViewerLoadResult firstPage = workflowService.loadFile(largeFile);
    String sessionId = firstPage.largePreviewSession().sessionId();
    JsonViewerLoadResult thirdPage =
        workflowService.loadLargePreviewPage(sessionId, 2).orElseThrow().loadResult();
    JsonViewerLoadResult backToFirstPage =
        workflowService.loadLargePreviewPage(sessionId, 0).orElseThrow().loadResult();
    JsonViewerLoadResult smallResult = workflowService.loadFile(smallFile);

    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.LARGE_PREVIEW,
        firstPage.inspectionMode());
    assertEquals(0, firstPage.largePreviewSession().currentPageIndex());
    assertTrue(firstPage.asciiTreeDocument().content().contains("page: 0"));
    assertEquals(2, thirdPage.largePreviewSession().currentPageIndex());
    assertTrue(thirdPage.asciiTreeDocument().content().contains("page: 2"));
    assertEquals(0, backToFirstPage.largePreviewSession().currentPageIndex());
    assertTrue(workflowService.currentLargePreviewOutlineDigest().isEmpty());
    assertEquals(
        com.davidpe.jsontree.application.model.JsonInspectionMode.FULL,
        smallResult.inspectionMode());
    assertTrue(smallResult.capabilities().rawJsonAvailable());
    assertTrue(smallResult.capabilities().searchAvailable());
    assertEquals(1, largePreviewStore.deletedSessionPaths.size());
    assertEquals(1, renderer.fullRenderCount);
  }

  @Test
  void returnsCurrentChunkAsRawJsonForLargePreviewMode() throws IOException {
    TrackingLargePreviewSessionStore largePreviewStore =
        new TrackingLargePreviewSessionStore(List.of("{\"chunk\":1}", "{\"chunk\":2}"), LargePreviewOutlineDigest.empty());
    JsonViewerWorkflowService workflowService =
        new JsonViewerWorkflowService(
            unusedValidationPort(),
            new InMemoryHistoryRepository(),
            unusedRendererPort(),
            inspectionModeResolver(8L),
            largePreviewSessionService(largePreviewStore));
    Path largeFile = Files.writeString(tempDir.resolve("chunked.json"), "{\"chunk\":1}{\"chunk\":2}");

    JsonViewerLoadResult firstPage = workflowService.loadFile(largeFile);
    assertEquals(firstPage.asciiTreeDocument().content(), workflowService.currentViewRawJson().orElseThrow());
    String sessionId = firstPage.largePreviewSession().sessionId();
    JsonViewerLoadResult secondPage =
        workflowService.loadLargePreviewPage(sessionId, 1).orElseThrow().loadResult();

    assertEquals(secondPage.asciiTreeDocument().content(), workflowService.currentViewRawJson().orElseThrow());
  }

  @Test
  void returnsEmptyWhenHistorySnapshotPathIsMissing() {
    InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
    repository.entry =
        new ImportedJsonFile(
            "2026-07-04_10-00-00_missing.json",
            "missing.json",
            Instant.parse("2026-07-04T10:00:00Z"),
            24L,
            3,
            true,
            false);
    JsonViewerWorkflowService workflowService =
        new JsonViewerWorkflowService(
            unusedValidationPort(),
            repository,
            path -> new AsciiTreeDocument("root", "root", 1),
            inspectionModeResolver(16L),
            largePreviewSessionService(new TrackingLargePreviewSessionStore()));

    assertTrue(workflowService.reopenHistoryEntry(repository.entry.storedName()).isEmpty());
  }

  private JsonValidationPort unusedValidationPort() {
    return path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
  }

  private AsciiTreeRendererPort unusedRendererPort() {
    return path -> null;
  }

  private JsonInspectionModeResolver inspectionModeResolver(long fullRenderMaxBytes) {
    return new JsonInspectionModeResolver(properties(fullRenderMaxBytes));
  }

  private LargePreviewProperties properties(long fullRenderMaxBytes) {
    LargePreviewProperties properties = new LargePreviewProperties();
    properties.setFullRenderMaxBytes(fullRenderMaxBytes);
    return properties;
  }

  private LargePreviewSessionService largePreviewSessionService(
      TrackingLargePreviewSessionStore largePreviewStore) {
    return largePreviewSessionService(largePreviewStore, source -> {});
  }

  private LargePreviewSessionService largePreviewSessionService(
      TrackingLargePreviewSessionStore largePreviewStore, SessionSourceHook sessionSourceHook) {
    return new LargePreviewSessionService(
        largePreviewStore.withHook(sessionSourceHook), 2, new DirectExecutorService());
  }

  private static final class InMemoryHistoryRepository implements JsonHistoryRepository {

    private ImportedJsonFile entry;
    private String storedJson;
    private Path storedJsonPath;

    @Override
    public List<ImportedJsonFile> findAll() {
      return List.of();
    }

    @Override
    public Optional<ImportedJsonFile> findByStoredName(String storedName) {
      return Optional.ofNullable(entry).filter(existing -> existing.storedName().equals(storedName));
    }

    @Override
    public Optional<Path> resolveStoredJsonPath(String storedName) {
      return entry != null && entry.storedName().equals(storedName)
          ? Optional.ofNullable(storedJsonPath)
          : Optional.empty();
    }

    @Override
    public Optional<String> readStoredJson(String storedName) {
      return entry != null && entry.storedName().equals(storedName)
          ? Optional.ofNullable(storedJson)
          : Optional.empty();
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

  private static final class TrackingRendererPort implements AsciiTreeRendererPort {

    private int fullRenderCount;
    private int largePreviewRenderCount;

    @Override
    public AsciiTreeDocument render(Path jsonFilePath) {
      fullRenderCount++;
      return new AsciiTreeDocument("root", "root\n├─ full: true", 2);
    }

    @Override
    public AsciiTreeDocument renderLargePreview(Path jsonFilePath) {
      largePreviewRenderCount++;
      return new AsciiTreeDocument("root", "root\n├─ preview: true", 2);
    }
  }

  @FunctionalInterface
  private interface SessionSourceHook {
    void accept(LargePreviewSessionSource source);
  }

  private static final class TrackingLargePreviewSessionStore
      implements LargePreviewSessionStorePort {

    private final List<Path> deletedSessionPaths = new ArrayList<>();
    private final List<String> pages;
    private final LargePreviewOutlineDigest outlineDigest;
    private SessionSourceHook sessionSourceHook = source -> {};
    private int materializeCalls;

    private TrackingLargePreviewSessionStore() {
      this(List.of("root\n├─ preview: true"), LargePreviewOutlineDigest.empty());
    }

    private TrackingLargePreviewSessionStore(
        List<String> pages, LargePreviewOutlineDigest outlineDigest) {
      this.pages = List.copyOf(pages);
      this.outlineDigest = outlineDigest;
    }

    private TrackingLargePreviewSessionStore withHook(SessionSourceHook sessionSourceHook) {
      this.sessionSourceHook = sessionSourceHook;
      return this;
    }

    @Override
    public LargePreviewMaterializationSnapshot materialize(
        String sessionId,
        LargePreviewSessionSource source,
        java.util.function.Consumer<LargePreviewPageDescriptor> onPageAvailable) {
      materializeCalls++;
      sessionSourceHook.accept(source);
      try {
        Path sessionDirectory = Files.createTempDirectory("json-tree-workflow-preview-");
        List<LargePreviewPageDescriptor> descriptors = new ArrayList<>();
        long logicalLineStart = 0L;
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
          String content = pages.get(pageIndex);
          Path pagePath = sessionDirectory.resolve("page-%05d.txt".formatted(pageIndex));
          Files.writeString(pagePath, content);
          int logicalLineCount = Math.max(1, content.split("\\R", -1).length);
          LargePreviewPageDescriptor descriptor =
              new LargePreviewPageDescriptor(
                  pageIndex, pagePath, logicalLineStart, logicalLineCount);
          descriptors.add(descriptor);
          logicalLineStart += logicalLineCount;
          onPageAvailable.accept(descriptor);
        }
        return new LargePreviewMaterializationSnapshot(
            sessionId,
            sessionDirectory,
            List.copyOf(descriptors),
            logicalLineStart,
            outlineDigest);
      } catch (IOException exception) {
        throw new IllegalStateException(exception);
      }
    }

    @Override
    public Optional<LargePreviewPageContent> readPage(LargePreviewPageDescriptor descriptor) {
      try {
        return Optional.of(
            new LargePreviewPageContent(descriptor, Files.readString(descriptor.storagePath())));
      } catch (IOException exception) {
        throw new IllegalStateException(exception);
      }
    }

    @Override
    public void deleteSessionStorage(Path sessionStoragePath) {
      deletedSessionPaths.add(sessionStoragePath);
    }
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

  private LargePreviewOutlineDigest outlineDigestForPageIndexes(int... pageIndexes) {
    List<com.davidpe.jsontree.application.model.LargePreviewOutlineDigestEntry> entries =
        new ArrayList<>();
    for (int pageIndex : pageIndexes) {
      entries.add(
          new com.davidpe.jsontree.application.model.LargePreviewOutlineDigestEntry(
              pageIndex,
              new com.davidpe.jsontree.application.model.JsonOutlineEntry(
                  0,
                  24,
                  com.davidpe.jsontree.application.model.JsonOutlineEntryKind.OBJECT,
                  0)));
    }
    return new LargePreviewOutlineDigest(entries, 0);
  }
}
