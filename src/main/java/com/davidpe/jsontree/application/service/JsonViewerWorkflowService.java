package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.port.in.ImportJsonUseCase;
import com.davidpe.jsontree.application.port.in.OpenHistoryUseCase;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonViewerWorkflowService implements ImportJsonUseCase, OpenHistoryUseCase {

  private static final DateTimeFormatter SNAPSHOT_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
          .withLocale(Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  private final JsonValidationPort validationPort;
  private final JsonHistoryRepository jsonHistoryRepository;
  private final AsciiTreeRendererPort asciiTreeRendererPort;
  private final Clock clock;

  private JsonViewerLoadResult currentView;

  @Autowired
  public JsonViewerWorkflowService(
      JsonValidationPort validationPort,
      JsonHistoryRepository jsonHistoryRepository,
      AsciiTreeRendererPort asciiTreeRendererPort) {
    this(validationPort, jsonHistoryRepository, asciiTreeRendererPort, Clock.systemDefaultZone());
  }

  JsonViewerWorkflowService(
      JsonValidationPort validationPort,
      JsonHistoryRepository jsonHistoryRepository,
      AsciiTreeRendererPort asciiTreeRendererPort,
      Clock clock) {
    this.validationPort = validationPort;
    this.jsonHistoryRepository = jsonHistoryRepository;
    this.asciiTreeRendererPort = asciiTreeRendererPort;
    this.clock = clock;
  }

  @Override
  public JsonImportResult importFile(Path jsonFilePath) {
    Path normalizedPath = jsonFilePath.toAbsolutePath().normalize();
    boolean exists = Files.exists(normalizedPath);
    boolean readable = Files.isReadable(normalizedPath);
    boolean regularFile = Files.isRegularFile(normalizedPath);

    return new JsonImportResult(
        normalizedPath,
        normalizedPath.getFileName().toString(),
        resolveSize(normalizedPath, exists, regularFile),
        exists,
        readable,
        regularFile);
  }

  public JsonViewerLoadResult loadFile(Path jsonFilePath) {
    JsonImportResult importResult = importFile(jsonFilePath);
    return loadImportedFile(importResult);
  }

  public JsonViewerLoadResult loadImportedFile(JsonImportResult importResult) {
    if (!importResult.available()) {
      JsonViewerLoadResult unavailableResult =
          new JsonViewerLoadResult(
              importResult,
              new JsonValidationResult(
                  JsonValidationStatus.PARSING_ERROR, "JSON file is not available.", null, null),
              null,
              null);
      currentView = unavailableResult;
      return unavailableResult;
    }

    JsonValidationResult validationResult = validationPort.validate(importResult.path());
    AsciiTreeDocument asciiTreeDocument = null;
    ImportedJsonFile historyEntry = null;

    if (validationResult.valid()) {
      asciiTreeDocument = asciiTreeRendererPort.render(importResult.path());
      historyEntry = createHistoryEntry(importResult, asciiTreeDocument);
      jsonHistoryRepository.save(historyEntry, readFileContents(importResult.path()));
    }

    JsonViewerLoadResult loadResult =
        new JsonViewerLoadResult(importResult, validationResult, asciiTreeDocument, historyEntry);
    currentView = loadResult;
    return loadResult;
  }

  public List<ImportedJsonFile> loadHistoryEntries() {
    return jsonHistoryRepository.findAll();
  }

  public Optional<JsonViewerLoadResult> reopenHistoryEntry(String storedName) {
    Optional<ImportedJsonFile> historyEntry = jsonHistoryRepository.findByStoredName(storedName);
    Optional<String> storedJson = jsonHistoryRepository.readStoredJson(storedName);
    if (historyEntry.isEmpty() || storedJson.isEmpty()) {
      return Optional.empty();
    }

    Path virtualPath = Path.of(historyEntry.get().storedName());
    JsonImportResult importResult =
        new JsonImportResult(
            virtualPath,
            historyEntry.get().originalName(),
            historyEntry.get().sizeBytes(),
            true,
            true,
            true);

    JsonValidationResult validationResult =
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
    AsciiTreeDocument asciiTreeDocument =
        asciiTreeRendererPort.render(writeTempSnapshot(storedJson.get(), storedName));
    JsonViewerLoadResult loadResult =
        new JsonViewerLoadResult(
            importResult, validationResult, asciiTreeDocument, historyEntry.get());
    currentView = loadResult;
    return Optional.of(loadResult);
  }

  public void deleteHistoryEntry(String storedName) {
    jsonHistoryRepository.deleteByStoredName(storedName);
    if (currentView != null
        && currentView.historyEntry() != null
        && storedName.equals(currentView.historyEntry().storedName())) {
      currentView = null;
    }
  }

  public Optional<JsonViewerLoadResult> currentView() {
    return Optional.ofNullable(currentView);
  }

  @Override
  public void openHistory() {
    throw new UnsupportedOperationException("Pending implementation.");
  }

  private long resolveSize(Path path, boolean exists, boolean regularFile) {
    if (!exists || !regularFile) {
      return 0L;
    }
    try {
      return Files.size(path);
    } catch (IOException exception) {
      return 0L;
    }
  }

  private ImportedJsonFile createHistoryEntry(
      JsonImportResult importResult, AsciiTreeDocument asciiTreeDocument) {
    Instant importedAt = Instant.now(clock);
    return new ImportedJsonFile(
        buildStoredName(importedAt, importResult.fileName()),
        importResult.fileName(),
        importedAt,
        importResult.sizeBytes(),
        asciiTreeDocument.lineCount(),
        true);
  }

  private String buildStoredName(Instant importedAt, String originalName) {
    String sanitizedName = sanitizeOriginalName(originalName);
    return SNAPSHOT_FORMATTER.format(importedAt) + "_" + sanitizedName;
  }

  private String sanitizeOriginalName(String originalName) {
    String fileName = Path.of(originalName).getFileName().toString();
    int extensionIndex = fileName.lastIndexOf('.');
    String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
    String extension = extensionIndex >= 0 ? fileName.substring(extensionIndex) : "";

    String sanitizedBaseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-{2,}", "-");
    if (sanitizedBaseName.isBlank()) {
      sanitizedBaseName = "snapshot";
    }
    return sanitizedBaseName + extension;
  }

  private String readFileContents(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read JSON file: " + path, exception);
    }
  }

  private Path writeTempSnapshot(String content, String storedName) {
    try {
      String suffix = storedName.endsWith(".json") ? ".json" : ".tmp";
      Path tempFile =
          Files.createTempFile(
              FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir")),
              "json-tree-history-",
              suffix);
      Files.writeString(tempFile, content);
      tempFile.toFile().deleteOnExit();
      return tempFile;
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Unable to prepare stored JSON snapshot: " + storedName, exception);
    }
  }
}
