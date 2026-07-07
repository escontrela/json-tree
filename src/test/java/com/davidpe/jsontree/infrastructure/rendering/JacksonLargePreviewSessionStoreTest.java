package com.davidpe.jsontree.infrastructure.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JacksonLargePreviewSessionStoreTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @TempDir Path tempDir;

  @Test
  void buildsByteIndexedChunkDescriptorsWithoutMaterializingWholePreview() throws Exception {
    Path jsonFile = Files.writeString(tempDir.resolve("large.json"), repeatedJson(420_000));
    JacksonLargePreviewSessionStore store = storeWithChunkConfig(150 * 1024, 12 * 1024, 512 * 1024);

    LargePreviewMaterializationSnapshot snapshot =
        store.materialize("session-1", LargePreviewSessionSource.local(jsonFile), descriptor -> {});

    assertEquals("session-1", snapshot.sessionId());
    assertTrue(Files.isDirectory(snapshot.sessionStoragePath()));
    assertEquals(Files.size(jsonFile), snapshot.totalLogicalLines());
    assertFalse(snapshot.pages().isEmpty());
    assertTrue(snapshot.indexOffsets().contains(0L));
    assertTrue(snapshot.outlineDigest().emptyDigest());
    assertEquals(
        jsonFile.toAbsolutePath().normalize(),
        snapshot.pages().getFirst().storagePath().toAbsolutePath().normalize());
    assertEquals(0L, snapshot.pages().getFirst().startingLogicalLine());
    assertEquals(150 * 1024, snapshot.pages().getFirst().logicalLineCount());
    assertEquals(0, snapshot.pages().getFirst().leadingOverlapBytes());
    assertEquals(12 * 1024, snapshot.pages().getFirst().trailingOverlapBytes());
  }

  @Test
  void readsOverlappingChunksWithoutLeavingByteGaps() throws Exception {
    Path jsonFile = Files.writeString(tempDir.resolve("source.json"), repeatedJson(360_000));
    JacksonLargePreviewSessionStore store = storeWithChunkConfig(150 * 1024, 12 * 1024, 512 * 1024);
    List<LargePreviewPageDescriptor> seenPages = new ArrayList<>();

    LargePreviewMaterializationSnapshot snapshot =
        store.materialize("session-overlap", LargePreviewSessionSource.local(jsonFile), seenPages::add);

    assertTrue(snapshot.pages().size() >= 2);
    LargePreviewPageDescriptor firstPage = seenPages.get(0);
    LargePreviewPageDescriptor secondPage = seenPages.get(1);
    String firstChunk = store.readPage(firstPage).orElseThrow().content();
    String secondChunk = store.readPage(secondPage).orElseThrow().content();
    String firstTail =
        firstChunk.substring(Math.max(0, firstChunk.length() - firstPage.trailingOverlapBytes()));
    String secondHead =
        secondChunk.substring(0, Math.min(secondChunk.length(), secondPage.leadingOverlapBytes()));

    assertEquals(12 * 1024, secondPage.leadingOverlapBytes());
    assertTrue(secondPage.startingLogicalLine() < firstPage.endingLogicalLineExclusive());
    assertEquals(
        firstPage.startingLogicalLine()
            + firstPage.logicalLineCount()
            - firstPage.trailingOverlapBytes(),
        secondPage.startingLogicalLine());
    assertEquals(firstTail, secondHead);
  }

  @Test
  void keepsChunkContinuityWhenUtf8CodePointsCrossPageBoundaries() throws Exception {
    Path jsonFile =
        Files.writeString(
            tempDir.resolve("utf8-source.json"),
            "{\"payload\":\"" + "áéíóú🙂漢字".repeat(80_000) + "\"}");
    JacksonLargePreviewSessionStore store =
        storeWithChunkConfig(16 * 1024, 1024, 512 * 1024);
    List<LargePreviewPageDescriptor> seenPages = new ArrayList<>();

    LargePreviewMaterializationSnapshot snapshot =
        store.materialize("session-utf8", LargePreviewSessionSource.local(jsonFile), seenPages::add);

    assertTrue(snapshot.pages().size() >= 2);
    LargePreviewPageDescriptor firstPage = seenPages.get(0);
    LargePreviewPageDescriptor secondPage = seenPages.get(1);
    String firstChunk = store.readPage(firstPage).orElseThrow().content();
    String secondChunk = store.readPage(secondPage).orElseThrow().content();

    assertFalse(firstChunk.isEmpty());
    assertFalse(secondChunk.isEmpty());
    assertEquals(
        firstPage.startingLogicalLine()
            + firstPage.logicalLineCount()
            - firstPage.trailingOverlapBytes(),
        secondPage.startingLogicalLine());
    assertTrue(firstChunk.endsWith(secondChunk.substring(0, Math.min(secondChunk.length(), 128))));
  }

  private JacksonLargePreviewSessionStore storeWithChunkConfig(
      int visibleChunkBytes, int overlapBytes, int pageIndexStrideBytes) {
    LargePreviewProperties properties = new LargePreviewProperties();
    properties.setVisibleChunkBytes(visibleChunkBytes);
    properties.setChunkOverlapBytes(overlapBytes);
    properties.setPageIndexStrideBytes(pageIndexStrideBytes);
    return new JacksonLargePreviewSessionStore(objectMapper, properties, tempDir);
  }

  private String repeatedJson(int payloadBytes) {
    String repeated = "0123456789abcdef".repeat(Math.max(1, payloadBytes / 16));
    return "{\"payload\":\"" + repeated + "\"}";
  }
}
