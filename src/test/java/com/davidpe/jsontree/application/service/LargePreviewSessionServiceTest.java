package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewOutlineDigest;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewPageLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewPagedSession;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.out.LargePreviewSessionStorePort;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LargePreviewSessionServiceTest {

  @TempDir Path tempDir;

  @Test
  void opensCompletedSessionWithKnownTotalsAndResidentWindow() throws Exception {
    TestLargePreviewSessionStore store =
        new TestLargePreviewSessionStore(
            tempDir,
            List.of("page-0", "page-1", "page-2", "page-3"),
            pageIndex -> {});
    try (ExecutorService executor = Executors.newCachedThreadPool()) {
      LargePreviewSessionService service = new LargePreviewSessionService(store, 2, executor);

      LargePreviewPageLoadResult firstPage = service.openSession(localSource());
      LargePreviewPagedSession session = service.session(firstPage.session().sessionId()).orElseThrow();

      assertEquals("page-0", firstPage.page().content());
      assertFalse(firstPage.cacheHit());
      assertFalse(firstPage.waitedForAvailability());
      assertTrue(firstPage.session().totalPagesKnown());
      assertEquals(1, store.materializeCalls());
      assertEquals(4, session.totalPages());
      assertTrue(session.hasDocumentRanges());
      assertEquals(List.of(0, 1, 2), residentPageIndexes(session));

      service.closeSession(session.sessionId());
      assertEquals(1, store.deletedSessionPaths().size());
    }
  }

  @Test
  void maintainsBoundedWarmWindowAcrossLongNavigation() throws Exception {
    TestLargePreviewSessionStore store =
        new TestLargePreviewSessionStore(
            tempDir,
            List.of("page-0", "page-1", "page-2", "page-3", "page-4", "page-5", "page-6", "page-7"),
            pageIndex -> {});
    try (ExecutorService executor = Executors.newCachedThreadPool()) {
      LargePreviewSessionService service = new LargePreviewSessionService(store, 2, executor);

      LargePreviewPageLoadResult firstPage = service.openSession(localSource());

      LargePreviewPageLoadResult pageThree =
          service.loadPage(firstPage.session().sessionId(), 3).orElseThrow();
      assertFalse(pageThree.cacheHit());
      assertEquals(
          List.of(1, 2, 3, 4, 5),
          residentPageIndexes(service.session(firstPage.session().sessionId()).orElseThrow()));

      service.loadPage(firstPage.session().sessionId(), 6).orElseThrow();
      assertEquals(
          List.of(4, 5, 6, 7),
          residentPageIndexes(service.session(firstPage.session().sessionId()).orElseThrow()));
    }
  }

  @Test
  void reportsColdDiskLoadsWithoutWaitsAndWarmCacheHitsAfterwards() throws Exception {
    TestLargePreviewSessionStore store =
        new TestLargePreviewSessionStore(
            tempDir,
            List.of("page-0", "page-1", "page-2", "page-3"),
            pageIndex -> {});
    try (ExecutorService executor = Executors.newCachedThreadPool()) {
      LargePreviewSessionService service = new LargePreviewSessionService(store, 0, executor);

      LargePreviewPageLoadResult firstPage = service.openSession(localSource());
      LargePreviewPageLoadResult coldPage =
          service.loadPage(firstPage.session().sessionId(), 2).orElseThrow();

      assertFalse(firstPage.waitedForAvailability());
      assertFalse(firstPage.cacheHit());
      assertFalse(coldPage.waitedForAvailability());
      assertFalse(coldPage.cacheHit());

      LargePreviewPageLoadResult warmPage =
          service.loadPage(firstPage.session().sessionId(), 2).orElseThrow();
      assertTrue(warmPage.cacheHit());
      assertFalse(warmPage.waitedForAvailability());
    }
  }

  @Test
  void closesAllOpenSessionsAndDeletesTheirTemporaryStorage() throws Exception {
    TestLargePreviewSessionStore store =
        new TestLargePreviewSessionStore(tempDir, List.of("page-0", "page-1"), pageIndex -> {});
    try (ExecutorService executor = Executors.newCachedThreadPool()) {
      LargePreviewSessionService service = new LargePreviewSessionService(store, 2, executor);

      LargePreviewPageLoadResult firstSession = service.openSession(localSource());
      LargePreviewPageLoadResult secondSession = service.openSession(localSource());

      service.closeAllSessions();

      assertTrue(service.session(firstSession.session().sessionId()).isEmpty());
      assertTrue(service.session(secondSession.session().sessionId()).isEmpty());
      assertEquals(2, store.deletedSessionPaths().size());
    }
  }

  private LargePreviewSessionSource localSource() throws IOException {
    Path jsonPath = Files.writeString(tempDir.resolve("source.json"), "{\"ok\":true}");
    return new LargePreviewSessionSource(jsonPath, JsonDocumentSourceKind.LOCAL_FILE, null);
  }

  private List<Integer> residentPageIndexes(LargePreviewPagedSession session) {
    return session.pageStates().stream()
        .filter(LargePreviewPageState -> LargePreviewPageState.residentInMemory())
        .map(LargePreviewPageState -> LargePreviewPageState.pageIndex())
        .toList();
  }

  @FunctionalInterface
  private interface PageHook {
    void afterPagePersisted(int pageIndex) throws Exception;
  }

  private static final class TestLargePreviewSessionStore implements LargePreviewSessionStorePort {

    private final Path tempDir;
    private final List<String> pages;
    private final PageHook pageHook;
    private final List<Path> deletedSessionPaths = new ArrayList<>();
    private final Map<Integer, LargePreviewPageDescriptor> descriptors = new LinkedHashMap<>();
    private int materializeCalls;

    private TestLargePreviewSessionStore(Path tempDir, List<String> pages, PageHook pageHook) {
      this.tempDir = tempDir;
      this.pages = pages;
      this.pageHook = pageHook;
    }

    @Override
    public LargePreviewMaterializationSnapshot materialize(
        String sessionId,
        LargePreviewSessionSource source,
        java.util.function.Consumer<LargePreviewPageDescriptor> onPageAvailable) {
      materializeCalls++;
      try {
        Path sessionDir = Files.createDirectories(tempDir.resolve(sessionId));
        long logicalLineStart = 0L;
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
          String content = pages.get(pageIndex);
          Path pagePath = sessionDir.resolve("page-%05d.txt".formatted(pageIndex));
          Files.writeString(pagePath, content);
          int logicalLineCount = Math.max(1, content.split("\\R", -1).length);
          LargePreviewPageDescriptor descriptor =
              new LargePreviewPageDescriptor(pageIndex, pagePath, logicalLineStart, logicalLineCount);
          descriptors.put(pageIndex, descriptor);
          logicalLineStart += logicalLineCount;
          onPageAvailable.accept(descriptor);
          pageHook.afterPagePersisted(pageIndex);
        }
        return new LargePreviewMaterializationSnapshot(
            sessionId,
            sessionDir,
            List.copyOf(descriptors.values()),
            logicalLineStart,
            LargePreviewOutlineDigest.empty());
      } catch (Exception exception) {
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

    private int materializeCalls() {
      return materializeCalls;
    }

    private List<Path> deletedSessionPaths() {
      return deletedSessionPaths;
    }
  }
}
