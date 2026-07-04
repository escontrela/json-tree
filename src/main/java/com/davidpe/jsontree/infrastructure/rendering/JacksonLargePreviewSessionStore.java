package com.davidpe.jsontree.infrastructure.rendering;

import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.out.LargePreviewSessionStorePort;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JacksonLargePreviewSessionStore implements LargePreviewSessionStorePort {

  private final ObjectMapper objectMapper;
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
    this.objectMapper = objectMapper;
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
    try (InputStream inputStream = Files.newInputStream(source.path());
        JsonParser parser = objectMapper.getFactory().createParser(inputStream)) {
      Files.createDirectories(sessionStoragePath);
      PagedPreviewWriter writer =
          new PagedPreviewWriter(
              sessionStoragePath, largePreviewProperties.getPageLineCount(), safeListener);
      writer.appendLine("root");

      JsonToken rootToken = parser.nextToken();
      if (rootToken == null) {
        return writer.finish(sessionId);
      }

      if (rootToken == JsonToken.START_OBJECT) {
        appendObject(writer, parser, "");
      } else if (rootToken == JsonToken.START_ARRAY) {
        appendArray(writer, parser, "");
      } else {
        writer.appendLine("├─ value: " + formatScalar(parser, rootToken));
      }
      return writer.finish(sessionId);
    } catch (IOException exception) {
      deleteSessionStorage(sessionStoragePath);
      throw new IllegalStateException(
          "Unable to materialize paged large-preview session for: " + source.path(), exception);
    }
  }

  @Override
  public Optional<LargePreviewPageContent> readPage(LargePreviewPageDescriptor descriptor) {
    try {
      if (!Files.exists(descriptor.storagePath())) {
        return Optional.empty();
      }
      return Optional.of(
          new LargePreviewPageContent(descriptor, Files.readString(descriptor.storagePath())));
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Unable to read large-preview page: " + descriptor.storagePath(), exception);
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

  private void appendObject(PagedPreviewWriter writer, JsonParser parser, String prefix)
      throws IOException {
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String fieldName = parser.getCurrentName();
      JsonToken valueToken = parser.nextToken();
      appendEntry(writer, parser, prefix, fieldName, valueToken);
    }
  }

  private void appendArray(PagedPreviewWriter writer, JsonParser parser, String prefix)
      throws IOException {
    int itemIndex = 0;
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      appendEntry(writer, parser, prefix, "[" + itemIndex + "]", parser.currentToken());
      itemIndex++;
    }
  }

  private void appendEntry(
      PagedPreviewWriter writer,
      JsonParser parser,
      String prefix,
      String label,
      JsonToken valueToken)
      throws IOException {
    if (valueToken == JsonToken.START_OBJECT) {
      writer.appendLine(prefix + "├─ " + label);
      appendObject(writer, parser, prefix + "│  ");
      return;
    }
    if (valueToken == JsonToken.START_ARRAY) {
      writer.appendLine(prefix + "├─ " + label + " [preview]");
      appendArray(writer, parser, prefix + "│  ");
      return;
    }
    writer.appendLine(prefix + "├─ " + label + ": " + formatScalar(parser, valueToken));
  }

  private String formatScalar(JsonParser parser, JsonToken token) throws IOException {
    return switch (token) {
      case VALUE_STRING -> "\"" + parser.getText() + "\"";
      case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT, VALUE_TRUE, VALUE_FALSE, VALUE_NULL ->
          parser.getText();
      default -> "\"<unsupported>\"";
    };
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

  private static final class PagedPreviewWriter {

    private final Path sessionStoragePath;
    private final int pageLineCount;
    private final Consumer<LargePreviewPageDescriptor> onPageAvailable;
    private final List<String> pendingLines = new ArrayList<>();
    private final List<LargePreviewPageDescriptor> pages = new ArrayList<>();
    private int pageIndex;
    private long totalLogicalLines;

    private PagedPreviewWriter(
        Path sessionStoragePath,
        int pageLineCount,
        Consumer<LargePreviewPageDescriptor> onPageAvailable) {
      this.sessionStoragePath = sessionStoragePath;
      this.pageLineCount = Math.max(1, pageLineCount);
      this.onPageAvailable = onPageAvailable;
    }

    private void appendLine(String line) throws IOException {
      pendingLines.add(line);
      totalLogicalLines++;
      if (pendingLines.size() >= pageLineCount) {
        flushPage();
      }
    }

    private LargePreviewMaterializationSnapshot finish(String sessionId) throws IOException {
      if (!pendingLines.isEmpty() || pages.isEmpty()) {
        flushPage();
      }
      return new LargePreviewMaterializationSnapshot(
          sessionId, sessionStoragePath, List.copyOf(pages), totalLogicalLines);
    }

    private void flushPage() throws IOException {
      Path pagePath = sessionStoragePath.resolve("page-%05d.txt".formatted(pageIndex));
      Files.writeString(pagePath, String.join("\n", pendingLines));
      LargePreviewPageDescriptor descriptor =
          new LargePreviewPageDescriptor(
              pageIndex,
              pagePath,
              totalLogicalLines - pendingLines.size(),
              pendingLines.size());
      pages.add(descriptor);
      pendingLines.clear();
      pageIndex++;
      onPageAvailable.accept(descriptor);
    }
  }
}
