package com.davidpe.jsontree.infrastructure.rendering;

import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewOutlineDigest;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.out.LargePreviewSessionStorePort;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Builds a byte-based large-preview session index and reads chunk content on demand from the
 * original JSON file.
 *
 * <p>The store keeps the existing port contract and temporary session root, but it no longer
 * materializes full ASCII pages in advance. Instead, it creates byte-window descriptors plus coarse
 * offset checkpoints and only reads the requested chunk from the source file when the workflow asks
 * for it.
 */
@Service
public class JacksonLargePreviewSessionStore implements LargePreviewSessionStorePort {

  private final LargePreviewProperties largePreviewProperties;
  private final Path tempRootDirectory;

  public JacksonLargePreviewSessionStore(ObjectMapper objectMapper) {
    this(objectMapper, new LargePreviewProperties());
  }

  @Autowired
  public JacksonLargePreviewSessionStore(
      ObjectMapper objectMapper, LargePreviewProperties largePreviewProperties) {
    this(objectMapper, largePreviewProperties, defaultTempRoot());
  }

  JacksonLargePreviewSessionStore(
      ObjectMapper objectMapper,
      LargePreviewProperties largePreviewProperties,
      Path tempRootDirectory) {
    this.largePreviewProperties = largePreviewProperties;
    this.tempRootDirectory = tempRootDirectory.toAbsolutePath().normalize();
  }

  @Override
  public LargePreviewMaterializationSnapshot materialize(
      String sessionId,
      LargePreviewSessionSource source,
      Consumer<LargePreviewPageDescriptor> onPageAvailable) {
    Consumer<LargePreviewPageDescriptor> safeListener =
        onPageAvailable == null ? descriptor -> {} : onPageAvailable;
    Path sessionStoragePath = createSessionStoragePath(sessionId);
    try {
      Files.createDirectories(sessionStoragePath);
      return buildChunkSnapshot(sessionId, source, sessionStoragePath, safeListener);
    } catch (IOException exception) {
      deleteSessionStorage(sessionStoragePath);
      throw new IllegalStateException(
          "Unable to materialize byte-indexed large-preview session for: " + source.path(),
          exception);
    }
  }

  @Override
  public Optional<LargePreviewPageContent> readPage(LargePreviewPageDescriptor descriptor) {
    try {
      if (!Files.exists(descriptor.storagePath())) {
        return Optional.empty();
      }
      return Optional.of(new LargePreviewPageContent(descriptor, readChunkText(descriptor)));
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Unable to read large-preview page chunk: " + descriptor.storagePath(), exception);
    }
  }

  @Override
  public void deleteSessionStorage(Path sessionStoragePath) {
    if (sessionStoragePath == null || !Files.exists(sessionStoragePath)) {
      return;
    }
    try (var paths = Files.walk(sessionStoragePath)) {
      paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException exception) {
                  throw new IllegalStateException(
                      "Unable to delete large-preview session path: " + path, exception);
                }
              });
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Unable to traverse large-preview session storage: " + sessionStoragePath, exception);
    }
  }

  private LargePreviewMaterializationSnapshot buildChunkSnapshot(
      String sessionId,
      LargePreviewSessionSource source,
      Path sessionStoragePath,
      Consumer<LargePreviewPageDescriptor> onPageAvailable)
      throws IOException {
    long fileSize = Files.size(source.path());
    int visibleChunkBytes = Math.max(1024, largePreviewProperties.getVisibleChunkBytes());
    int overlapBytes =
        Math.max(
            0, Math.min(largePreviewProperties.getChunkOverlapBytes(), visibleChunkBytes - 1024));

    List<Long> indexOffsets =
        buildIndexOffsets(fileSize, Math.max(1024, largePreviewProperties.getPageIndexStrideBytes()));
    List<LargePreviewPageDescriptor> pages = new ArrayList<>();

    if (fileSize == 0L) {
      LargePreviewPageDescriptor emptyDescriptor =
          new LargePreviewPageDescriptor(0, source.path(), 0L, 0, 0, 0);
      pages.add(emptyDescriptor);
      onPageAvailable.accept(emptyDescriptor);
      return new LargePreviewMaterializationSnapshot(
          sessionId,
          sessionStoragePath,
          pages,
          0L,
          indexOffsets,
          LargePreviewOutlineDigest.empty());
    }

    try (SeekableByteChannel channel = Files.newByteChannel(source.path())) {
      long startOffset = 0L;
      long previousEndOffset = 0L;
      int pageIndex = 0;
      while (startOffset < fileSize) {
        long desiredEndOffset = Math.min(fileSize, startOffset + visibleChunkBytes);
        long safeEndOffset =
            normalizeUtf8BoundaryAtOrBefore(channel, startOffset, desiredEndOffset, fileSize);
        if (safeEndOffset <= startOffset) {
          safeEndOffset = Math.min(fileSize, startOffset + visibleChunkBytes);
        }

        long nextStartOffset = safeEndOffset;
        if (safeEndOffset < fileSize) {
          long desiredNextStartOffset = Math.max(startOffset + 1L, safeEndOffset - overlapBytes);
          nextStartOffset =
              normalizeUtf8BoundaryAtOrBefore(channel, startOffset, desiredNextStartOffset, fileSize);
          if (nextStartOffset >= safeEndOffset) {
            nextStartOffset = Math.max(startOffset, safeEndOffset - overlapBytes);
          }
        }

        int byteCount = Math.toIntExact(safeEndOffset - startOffset);
        int leadingOverlap = pageIndex == 0 ? 0 : Math.toIntExact(previousEndOffset - startOffset);
        int trailingOverlap =
            safeEndOffset >= fileSize ? 0 : Math.toIntExact(safeEndOffset - nextStartOffset);
        LargePreviewPageDescriptor descriptor =
            new LargePreviewPageDescriptor(
                pageIndex,
                source.path(),
                startOffset,
                byteCount,
                leadingOverlap,
                trailingOverlap);
        pages.add(descriptor);
        onPageAvailable.accept(descriptor);
        pageIndex++;
        previousEndOffset = safeEndOffset;
        startOffset = nextStartOffset;
      }
    }

    return new LargePreviewMaterializationSnapshot(
        sessionId,
        sessionStoragePath,
        pages,
        fileSize,
        indexOffsets,
        LargePreviewOutlineDigest.empty());
  }

  private List<Long> buildIndexOffsets(long fileSize, int strideBytes) {
    long safeStride = Math.max(1024L, strideBytes);
    List<Long> offsets = new ArrayList<>();
    for (long offset = 0L; offset < fileSize; offset += safeStride) {
      offsets.add(offset);
    }
    if (offsets.isEmpty()) {
      offsets.add(0L);
    }
    return offsets;
  }

  private String readChunkText(LargePreviewPageDescriptor descriptor) throws IOException {
    if (descriptor.logicalLineCount() == 0) {
      return "";
    }
    try (SeekableByteChannel channel = Files.newByteChannel(descriptor.storagePath())) {
      channel.position(descriptor.startingLogicalLine());
      ByteBuffer buffer = ByteBuffer.allocate(descriptor.logicalLineCount());
      while (buffer.hasRemaining()) {
        if (channel.read(buffer) < 0) {
          break;
        }
      }
      return new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);
    }
  }

  private long normalizeUtf8BoundaryAtOrBefore(
      SeekableByteChannel channel, long startOffset, long candidateOffset, long fileSize)
      throws IOException {
    long normalizedOffset = Math.max(startOffset, Math.min(candidateOffset, fileSize));
    if (normalizedOffset == startOffset || normalizedOffset == fileSize) {
      return normalizedOffset;
    }
    while (normalizedOffset > startOffset
        && !isUtf8Boundary(channel, startOffset, normalizedOffset, fileSize)) {
      normalizedOffset--;
    }
    return normalizedOffset;
  }

  private boolean isUtf8Boundary(
      SeekableByteChannel channel, long startOffset, long boundaryOffset, long fileSize)
      throws IOException {
    if (boundaryOffset <= startOffset || boundaryOffset >= fileSize) {
      return true;
    }

    int bytesToInspect = (int) Math.min(4L, boundaryOffset - startOffset);
    long inspectionStart = boundaryOffset - bytesToInspect;
    byte[] tailBytes = readBytes(channel, inspectionStart, bytesToInspect);
    if (tailBytes.length == 0) {
      return true;
    }

    for (int index = tailBytes.length - 1; index >= 0; index--) {
      if (isUtf8ContinuationByte(tailBytes[index])) {
        continue;
      }
      int expectedLength = utf8SequenceLength(tailBytes[index]);
      int actualLength = tailBytes.length - index;
      return expectedLength == actualLength;
    }
    return false;
  }

  private byte[] readBytes(SeekableByteChannel channel, long startOffset, int byteCount)
      throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(Math.max(0, byteCount));
    channel.position(startOffset);
    while (buffer.hasRemaining()) {
      if (channel.read(buffer) < 0) {
        break;
      }
    }
    byte[] bytes = new byte[buffer.position()];
    System.arraycopy(buffer.array(), 0, bytes, 0, bytes.length);
    return bytes;
  }

  private boolean isUtf8ContinuationByte(byte value) {
    return (value & 0xC0) == 0x80;
  }

  private int utf8SequenceLength(byte value) {
    int unsignedValue = value & 0xFF;
    if ((unsignedValue & 0x80) == 0) {
      return 1;
    }
    if ((unsignedValue & 0xE0) == 0xC0) {
      return 2;
    }
    if ((unsignedValue & 0xF0) == 0xE0) {
      return 3;
    }
    if ((unsignedValue & 0xF8) == 0xF0) {
      return 4;
    }
    return 1;
  }

  private Path createSessionStoragePath(String sessionId) {
    try {
      Files.createDirectories(tempRootDirectory);
      return Files.createTempDirectory(
          tempRootDirectory, "json-tree-large-preview-" + sessionId + "-");
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Unable to create temporary storage for large-preview session: " + sessionId,
          exception);
    }
  }

  private static Path defaultTempRoot() {
    return Path.of(System.getProperty("java.io.tmpdir"));
  }
}
