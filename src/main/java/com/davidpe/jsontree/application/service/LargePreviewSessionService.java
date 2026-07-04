package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewOutlineDigest;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewPageLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewPageState;
import com.davidpe.jsontree.application.model.LargePreviewPagedSession;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.out.LargePreviewSessionStorePort;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LargePreviewSessionService {

  private final LargePreviewSessionStorePort sessionStorePort;
  private final int warmPageRadius;
  private final ExecutorService executorService;
  private final ConcurrentMap<String, RuntimeSession> sessions = new ConcurrentHashMap<>();

  @Autowired
  public LargePreviewSessionService(
      LargePreviewSessionStorePort sessionStorePort, LargePreviewProperties largePreviewProperties) {
    this(
        sessionStorePort,
        largePreviewProperties.getWarmPageRadius(),
        Executors.newCachedThreadPool(
            runnable -> {
              Thread thread = new Thread(runnable, "json-tree-large-preview");
              thread.setDaemon(true);
              return thread;
            }));
  }

  LargePreviewSessionService(
      LargePreviewSessionStorePort sessionStorePort,
      int warmPageRadius,
      ExecutorService executorService) {
    this.sessionStorePort = sessionStorePort;
    this.warmPageRadius = Math.max(0, warmPageRadius);
    this.executorService = executorService;
  }

  public LargePreviewPageLoadResult openSession(LargePreviewSessionSource source) {
    String sessionId = UUID.randomUUID().toString();
    RuntimeSession runtimeSession =
        new RuntimeSession(LargePreviewPagedSession.initializing(sessionId, source));
    RuntimeSession previous = sessions.putIfAbsent(sessionId, runtimeSession);
    if (previous != null) {
      throw new IllegalStateException("Duplicate large-preview session id generated: " + sessionId);
    }
    runtimeSession.materializationTask =
        executorService.submit(() -> materializeInBackground(runtimeSession));
    return loadPageInternal(runtimeSession, 0).orElseThrow();
  }

  public Optional<LargePreviewPageLoadResult> loadPage(String sessionId, int pageIndex) {
    RuntimeSession runtimeSession = sessions.get(sessionId);
    if (runtimeSession == null) {
      return Optional.empty();
    }
    return loadPageInternal(runtimeSession, pageIndex);
  }

  public Optional<LargePreviewPagedSession> session(String sessionId) {
    RuntimeSession runtimeSession = sessions.get(sessionId);
    if (runtimeSession == null) {
      return Optional.empty();
    }
    synchronized (runtimeSession.monitor) {
      return Optional.of(runtimeSession.session);
    }
  }

  public Optional<LargePreviewOutlineDigest> outlineDigest(String sessionId) {
    RuntimeSession runtimeSession = sessions.get(sessionId);
    if (runtimeSession == null) {
      return Optional.empty();
    }
    synchronized (runtimeSession.monitor) {
      return Optional.ofNullable(runtimeSession.outlineDigest);
    }
  }

  public void closeSession(String sessionId) {
    RuntimeSession runtimeSession = sessions.remove(sessionId);
    if (runtimeSession == null) {
      return;
    }
    if (runtimeSession.materializationTask != null) {
      runtimeSession.materializationTask.cancel(true);
    }
    synchronized (runtimeSession.monitor) {
      runtimeSession.closed = true;
      runtimeSession.session = runtimeSession.session.close();
      runtimeSession.monitor.notifyAll();
    }
    Path sessionStoragePath = runtimeSession.sessionStoragePath;
    if (sessionStoragePath != null) {
      sessionStorePort.deleteSessionStorage(sessionStoragePath);
    }
  }

  public void closeAllSessions() {
    new ArrayList<>(sessions.keySet()).forEach(this::closeSession);
  }

  private Optional<LargePreviewPageLoadResult> loadPageInternal(
      RuntimeSession runtimeSession, int pageIndex) {
    boolean waitedForAvailability = false;
    boolean cacheHit = false;
    LargePreviewPageContent page;

    synchronized (runtimeSession.monitor) {
      if (runtimeSession.closed) {
        return Optional.empty();
      }
      runtimeSession.session = applyRequestedWindow(runtimeSession.session, pageIndex);

      page = runtimeSession.warmPages.get(pageIndex);
      if (page != null) {
        cacheHit = true;
      } else {
        while (!runtimeSession.pageDescriptors.containsKey(pageIndex)
            && runtimeSession.failure == null
            && !runtimeSession.materializationFinished
            && !runtimeSession.closed) {
          waitedForAvailability = true;
          try {
            runtimeSession.monitor.wait();
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while waiting for large-preview page " + pageIndex,
                exception);
          }
        }
        if (runtimeSession.closed) {
          return Optional.empty();
        }
        if (runtimeSession.failure != null) {
          throw runtimeSession.failure;
        }
        LargePreviewPageDescriptor descriptor = runtimeSession.pageDescriptors.get(pageIndex);
        if (descriptor == null) {
          return Optional.empty();
        }
        page = sessionStorePort.readPage(descriptor).orElseThrow();
        runtimeSession.warmPages.put(pageIndex, page);
      }

      runtimeSession.session = runtimeSession.session.withCurrentPageIndex(pageIndex);
      prefetchWarmWindow(runtimeSession, pageIndex);
      evictOutsideWarmWindow(runtimeSession, pageIndex);
      refreshPageStates(runtimeSession);
      return Optional.of(
          new LargePreviewPageLoadResult(
              runtimeSession.session, runtimeSession.warmPages.get(pageIndex), cacheHit, waitedForAvailability));
    }
  }

  private void materializeInBackground(RuntimeSession runtimeSession) {
    LargePreviewPagedSession initialSession;
    synchronized (runtimeSession.monitor) {
      initialSession = runtimeSession.session;
    }

    try {
      LargePreviewMaterializationSnapshot snapshot =
          sessionStorePort.materialize(
              initialSession.sessionId(),
              initialSession.source(),
              descriptor -> onPageMaterialized(runtimeSession, descriptor));
      synchronized (runtimeSession.monitor) {
        runtimeSession.snapshot = snapshot;
        runtimeSession.outlineDigest = snapshot.outlineDigest();
        runtimeSession.materializationFinished = true;
        runtimeSession.sessionStoragePath = snapshot.sessionStoragePath();
        runtimeSession.session =
            runtimeSession.session
                .withKnownTotals(snapshot.totalPages(), snapshot.totalLogicalLines())
                .withOutlineDigestReady(!snapshot.outlineDigest().emptyDigest());
        refreshPageStates(runtimeSession);
        runtimeSession.monitor.notifyAll();
      }
    } catch (RuntimeException exception) {
      synchronized (runtimeSession.monitor) {
        runtimeSession.failure = exception;
        runtimeSession.monitor.notifyAll();
      }
    }
  }

  private void onPageMaterialized(
      RuntimeSession runtimeSession, LargePreviewPageDescriptor descriptor) {
    synchronized (runtimeSession.monitor) {
      if (runtimeSession.closed) {
        return;
      }
      runtimeSession.pageDescriptors.put(descriptor.pageIndex(), descriptor);
      runtimeSession.sessionStoragePath = descriptor.storagePath().getParent();
      runtimeSession.session =
          runtimeSession.session.withPageState(
              LargePreviewPageState.available(
                  descriptor.pageIndex(), false, true, descriptor.logicalLineCount()));
      if (withinWarmWindow(
          descriptor.pageIndex(), runtimeSession.session.currentPageIndex(), warmPageRadius)) {
        LargePreviewPageContent page = sessionStorePort.readPage(descriptor).orElseThrow();
        runtimeSession.warmPages.put(descriptor.pageIndex(), page);
        evictOutsideWarmWindow(runtimeSession, runtimeSession.session.currentPageIndex());
        refreshPageStates(runtimeSession);
      }
      runtimeSession.monitor.notifyAll();
    }
  }

  private void prefetchWarmWindow(RuntimeSession runtimeSession, int currentPageIndex) {
    int startIndex = Math.max(0, currentPageIndex - warmPageRadius);
    int endIndex = currentPageIndex + warmPageRadius;
    for (int pageIndex = startIndex; pageIndex <= endIndex; pageIndex++) {
      LargePreviewPageDescriptor descriptor = runtimeSession.pageDescriptors.get(pageIndex);
      if (descriptor == null || runtimeSession.warmPages.containsKey(pageIndex)) {
        continue;
      }
      LargePreviewPageContent page = sessionStorePort.readPage(descriptor).orElseThrow();
      runtimeSession.warmPages.put(pageIndex, page);
    }
  }

  private void evictOutsideWarmWindow(RuntimeSession runtimeSession, int currentPageIndex) {
    int startIndex = Math.max(0, currentPageIndex - warmPageRadius);
    int endIndex = currentPageIndex + warmPageRadius;
    runtimeSession.warmPages.entrySet().removeIf(entry -> entry.getKey() < startIndex || entry.getKey() > endIndex);
  }

  private void refreshPageStates(RuntimeSession runtimeSession) {
    LargePreviewPagedSession session = runtimeSession.session;
    for (LargePreviewPageDescriptor descriptor : runtimeSession.pageDescriptors.values()) {
      session =
          session.withPageState(
              LargePreviewPageState.available(
                  descriptor.pageIndex(),
                  runtimeSession.warmPages.containsKey(descriptor.pageIndex()),
                  true,
                  descriptor.logicalLineCount()));
    }
    runtimeSession.session = session;
  }

  private LargePreviewPagedSession applyRequestedWindow(
      LargePreviewPagedSession session, int currentPageIndex) {
    LargePreviewPagedSession nextSession = session;
    int startIndex = Math.max(0, currentPageIndex - warmPageRadius);
    int endIndex = currentPageIndex + warmPageRadius;
    for (int pageIndex = startIndex; pageIndex <= endIndex; pageIndex++) {
      if (nextSession.pageState(pageIndex).isEmpty()) {
        nextSession = nextSession.withPageState(LargePreviewPageState.requested(pageIndex));
      }
    }
    return nextSession;
  }

  private boolean withinWarmWindow(int pageIndex, int currentPageIndex, int radius) {
    return pageIndex >= Math.max(0, currentPageIndex - radius) && pageIndex <= currentPageIndex + radius;
  }

  private static final class RuntimeSession {

    private final Object monitor = new Object();
    private final Map<Integer, LargePreviewPageDescriptor> pageDescriptors = new LinkedHashMap<>();
    private final Map<Integer, LargePreviewPageContent> warmPages = new LinkedHashMap<>();
    private LargePreviewPagedSession session;
    private LargePreviewMaterializationSnapshot snapshot;
    private LargePreviewOutlineDigest outlineDigest;
    private RuntimeException failure;
    private java.util.concurrent.Future<?> materializationTask;
    private boolean materializationFinished;
    private boolean closed;
    private Path sessionStoragePath;

    private RuntimeSession(LargePreviewPagedSession session) {
      this.session = session;
    }
  }
}
