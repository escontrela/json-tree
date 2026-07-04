package com.davidpe.jsontree.infrastructure.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JacksonLargePreviewSessionStoreTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @TempDir Path tempDir;

  @Test
  void materializesOrderedMultiPageAsciiPreviewIntoTemporaryStorage() throws Exception {
    Path jsonFile =
        Files.writeString(
            tempDir.resolve("large.json"),
            """
            {
              "app": {
                "name": "json-tree",
                "version": "1.0.0",
                "themes": ["dark", "light"]
              },
              "user": {
                "id": 1,
                "roles": ["dev", "ops"]
              },
              "flags": {
                "active": true,
                "beta": false
              }
            }
            """);
    JacksonLargePreviewSessionStore store = storeWithPageSize(5);

    LargePreviewMaterializationSnapshot snapshot =
        store.materialize(
            "session-1", LargePreviewSessionSource.local(jsonFile), descriptor -> {});

    assertEquals("session-1", snapshot.sessionId());
    assertTrue(Files.isDirectory(snapshot.sessionStoragePath()));
    assertEquals(
        List.of(0, 1, 2),
        snapshot.pages().stream().map(LargePreviewPageDescriptor::pageIndex).toList());
    assertEquals(
        List.of(5, 5, 5),
        snapshot.pages().stream().map(LargePreviewPageDescriptor::logicalLineCount).toList());
    assertEquals(
        List.of(0L, 5L, 10L),
        snapshot.pages().stream().map(LargePreviewPageDescriptor::startingLogicalLine).toList());
    assertEquals(
        List.of(
            """
            root
            ├─ app
            │  ├─ name: "json-tree"
            │  ├─ version: "1.0.0"
            │  ├─ themes [preview]""",
            """
            │  │  ├─ [0]: "dark"
            │  │  ├─ [1]: "light"
            ├─ user
            │  ├─ id: 1
            │  ├─ roles [preview]""",
            """
            │  │  ├─ [0]: "dev"
            │  │  ├─ [1]: "ops"
            ├─ flags
            │  ├─ active: true
            │  ├─ beta: false"""),
        snapshot.pages().stream()
            .map(store::readPage)
            .map(optional -> optional.orElseThrow().content())
            .toList());
    assertEquals(15L, snapshot.totalLogicalLines());
  }

  @Test
  void exposesFirstPageBeforeFullMaterializationFinishes() throws Exception {
    Path jsonFile =
        Files.writeString(
            tempDir.resolve("large-early-page.json"),
            """
            {
              "nodes": [
                {"id": 1, "name": "alpha"},
                {"id": 2, "name": "beta"},
                {"id": 3, "name": "gamma"},
                {"id": 4, "name": "delta"}
              ],
              "meta": {
                "count": 4
              }
            }
            """);
    JacksonLargePreviewSessionStore store = storeWithPageSize(4);
    CountDownLatch firstPageAvailable = new CountDownLatch(1);
    CountDownLatch releaseFirstPage = new CountDownLatch(1);
    List<LargePreviewPageDescriptor> seenPages = new ArrayList<>();

    try (var executor = Executors.newSingleThreadExecutor()) {
      CompletableFuture<LargePreviewMaterializationSnapshot> future =
          CompletableFuture.supplyAsync(
              () ->
                  store.materialize(
                      "session-early",
                      LargePreviewSessionSource.local(jsonFile),
                      descriptor -> {
                        seenPages.add(descriptor);
                        if (descriptor.pageIndex() == 0) {
                          firstPageAvailable.countDown();
                          try {
                            releaseFirstPage.await(2, TimeUnit.SECONDS);
                          } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(exception);
                          }
                        }
                      }),
              executor);

      assertTrue(firstPageAvailable.await(2, TimeUnit.SECONDS));
      LargePreviewPageDescriptor firstPage = seenPages.get(0);
      assertNotNull(firstPage);
      assertTrue(Files.exists(firstPage.storagePath()));
      assertFalse(future.isDone());
      assertTrue(
          store
              .readPage(firstPage)
              .orElseThrow()
              .content()
              .startsWith("root\n├─ nodes [preview]"));

      releaseFirstPage.countDown();
      LargePreviewMaterializationSnapshot snapshot = future.get(2, TimeUnit.SECONDS);
      assertTrue(snapshot.totalPages() >= 2);
    }
  }

  private JacksonLargePreviewSessionStore storeWithPageSize(int pageLineCount) {
    LargePreviewProperties properties = new LargePreviewProperties();
    properties.setPageLineCount(pageLineCount);
    return new JacksonLargePreviewSessionStore(objectMapper, properties, tempDir);
  }
}
