package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.HistoryJsonImportResult;
import com.davidpe.jsontree.application.model.HistoryJsonImportStatus;
import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewPageLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewPagedSession;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.application.port.in.ImportJsonUseCase;
import com.davidpe.jsontree.application.port.in.OpenHistoryUseCase;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import java.io.IOException;
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

/**
 * Orchestration layer for the JSON viewer workflow use case.
 *
 * <p>This service coordinates the end-to-end flow for opening/importing JSON sources and preparing
 * data for the UI layer. It delegates persistence concerns to {@link JsonHistoryRepository},
 * inspection-mode decisions to {@link JsonInspectionModeResolver}, validation to {@link
 * JsonValidationPort}, and rendering to {@link AsciiTreeRendererPort}.
 *
 * <p>It is the workflow director: it does not replace repositories or pure decision components, but
 * composes them to execute viewer actions from the input layer.
 */
@Service
public class JsonViewerWorkflowService implements ImportJsonUseCase, OpenHistoryUseCase {

  private static final DateTimeFormatter SNAPSHOT_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
          .withLocale(Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  private final JsonValidationPort validationPort;
  private final JsonHistoryRepository jsonHistoryRepository;
  private final AsciiTreeRendererPort asciiTreeRendererPort;
  private final JsonInspectionModeResolver inspectionModeResolver;
  private final LargePreviewSessionService largePreviewSessionService;
  private final Clock clock;

  private JsonViewerLoadResult currentView;

  @Autowired
  /** Creates the workflow service with the default system clock. */
  public JsonViewerWorkflowService(
      JsonValidationPort validationPort,
      JsonHistoryRepository jsonHistoryRepository,
      AsciiTreeRendererPort asciiTreeRendererPort,
      JsonInspectionModeResolver inspectionModeResolver,
      LargePreviewSessionService largePreviewSessionService) {
    this(
        validationPort,
        jsonHistoryRepository,
        asciiTreeRendererPort,
        inspectionModeResolver,
        largePreviewSessionService,
        Clock.systemDefaultZone());
  }

  JsonViewerWorkflowService(
      JsonValidationPort validationPort,
      JsonHistoryRepository jsonHistoryRepository,
      AsciiTreeRendererPort asciiTreeRendererPort,
      JsonInspectionModeResolver inspectionModeResolver,
      LargePreviewSessionService largePreviewSessionService,
      Clock clock) {
    this.validationPort = validationPort;
    this.jsonHistoryRepository = jsonHistoryRepository;
    this.asciiTreeRendererPort = asciiTreeRendererPort;
    this.inspectionModeResolver = inspectionModeResolver;
    this.largePreviewSessionService = largePreviewSessionService;
    this.clock = clock;
  }

  /**
   * Inspects a JSON file path and returns import state metadata.
   *
   * @param jsonFilePath candidate JSON source path.
   * @return import/read state used by subsequent workflow steps.
   */
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
        regularFile,
        JsonDocumentSourceKind.LOCAL_FILE);
  }

  /**
   * Imports and loads a JSON file in a single workflow entry point.
   *
   * @param jsonFilePath JSON source path.
   * @return viewer load result ready for the UI/controller.
   */
  public JsonViewerLoadResult loadFile(Path jsonFilePath) {
    JsonImportResult importResult = importFile(jsonFilePath);
    return loadImportedFile(importResult);
  }

  /**
   * Persists a validated import into history and returns operation status.
   *
   * @param importResult import/read state to process.
   * @return history import result with success or failure status.
   */
  public HistoryJsonImportResult importIntoHistory(JsonImportResult importResult) {
    JsonInspectionMode inspectionMode = inspectionModeResolver.resolve(importResult);
    if (!importResult.available()) {
      return HistoryJsonImportResult.failure(
          HistoryJsonImportStatus.UNREADABLE_FILE, "Selected JSON file is not available.");
    }

    JsonValidationResult validationResult = validationPort.validate(importResult.path());
    if (validationResult.status() == JsonValidationStatus.EMPTY) {
      return HistoryJsonImportResult.failure(
          HistoryJsonImportStatus.EMPTY_JSON, "Selected JSON file is empty.");
    }
    if (!validationResult.valid()) {
      return HistoryJsonImportResult.failure(
          validationResult.status() == JsonValidationStatus.INVALID
              ? HistoryJsonImportStatus.INVALID_JSON
              : HistoryJsonImportStatus.UNREADABLE_FILE,
          composeValidationMessage(validationResult));
    }

    AsciiTreeDocument asciiTreeDocument = renderDocument(importResult.path(), inspectionMode);
    ImportedJsonFile historyEntry = createHistoryEntry(importResult, asciiTreeDocument);
    jsonHistoryRepository.save(historyEntry, readFileContents(importResult.path()));
    return HistoryJsonImportResult.imported(historyEntry);
  }

  /**
   * Completes the load workflow for a previously inspected JSON source.
   *
   * @param importResult import/read state to load.
   * @return full viewer state, including validation and rendered data when available.
   */
  public JsonViewerLoadResult loadImportedFile(JsonImportResult importResult) {
    JsonInspectionMode inspectionMode = inspectionModeResolver.resolve(importResult);
    if (!importResult.available()) {
      JsonViewerLoadResult unavailableResult =
          new JsonViewerLoadResult(
              importResult,
              new JsonValidationResult(
                  JsonValidationStatus.PARSING_ERROR, "JSON file is not available.", null, null),
              null,
              null,
              inspectionMode,
              capabilitiesFor(inspectionMode),
              null);
      replaceCurrentView(unavailableResult);
      return unavailableResult;
    }

    JsonValidationResult validationResult = validationPort.validate(importResult.path());
    AsciiTreeDocument asciiTreeDocument = null;
    ImportedJsonFile historyEntry = null;
    LargePreviewPagedSession largePreviewSession = null;

    if (validationResult.valid()) {
      if (inspectionMode == JsonInspectionMode.LARGE_PREVIEW) {
        LargePreviewPageLoadResult pageLoadResult =
            largePreviewSessionService.openSession(sourceFor(importResult, null));
        asciiTreeDocument = toAsciiTreeDocument(pageLoadResult);
        largePreviewSession = pageLoadResult.session();
      } else {
        asciiTreeDocument = renderDocument(importResult.path(), inspectionMode);
      }
      historyEntry = createHistoryEntry(importResult, asciiTreeDocument);
      jsonHistoryRepository.save(historyEntry, readFileContents(importResult.path()));
    }

    JsonViewerLoadResult loadResult =
        new JsonViewerLoadResult(
            importResult,
            validationResult,
            asciiTreeDocument,
            historyEntry,
            inspectionMode,
            capabilitiesFor(inspectionMode),
            largePreviewSession);
    replaceCurrentView(loadResult);
    return loadResult;
  }

  /**
   * Retrieves the persisted JSON history list.
   *
   * @return all registered history entries.
   */
  public List<ImportedJsonFile> loadHistoryEntries() {
    return jsonHistoryRepository.findAll();
  }

  /**
   * Reopens a stored history entry and rebuilds the viewer state.
   *
   * @param storedName persisted history file name.
   * @return viewer state for the history entry when it exists.
   */
  public Optional<JsonViewerLoadResult> reopenHistoryEntry(String storedName) {
    Optional<ImportedJsonFile> historyEntry = jsonHistoryRepository.findByStoredName(storedName);
    Optional<Path> storedJsonPath = jsonHistoryRepository.resolveStoredJsonPath(storedName);
    if (historyEntry.isEmpty() || storedJsonPath.isEmpty()) {
      return Optional.empty();
    }
    JsonInspectionMode inspectionMode = inspectionModeResolver.resolve(historyEntry.get());

    JsonImportResult importResult =
        new JsonImportResult(
            storedJsonPath.get(),
            historyEntry.get().originalName(),
            historyEntry.get().sizeBytes(),
            true,
            true,
            true,
            JsonDocumentSourceKind.HISTORY);

    JsonValidationResult validationResult =
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
    AsciiTreeDocument asciiTreeDocument;
    LargePreviewPagedSession largePreviewSession = null;
    if (inspectionMode == JsonInspectionMode.LARGE_PREVIEW) {
      LargePreviewPageLoadResult pageLoadResult =
          largePreviewSessionService.openSession(
              LargePreviewSessionSource.history(storedJsonPath.get(), storedName));
      asciiTreeDocument = toAsciiTreeDocument(pageLoadResult);
      largePreviewSession = pageLoadResult.session();
    } else {
      asciiTreeDocument = renderDocument(storedJsonPath.get(), inspectionMode);
    }
    JsonViewerLoadResult loadResult =
        new JsonViewerLoadResult(
            importResult,
            validationResult,
            asciiTreeDocument,
            historyEntry.get(),
            inspectionMode,
            capabilitiesFor(inspectionMode),
            largePreviewSession);
    replaceCurrentView(loadResult);
    return Optional.of(loadResult);
  }

  /**
   * Deletes a history entry and clears current view if it points to that entry.
   *
   * @param storedName persisted history file name.
   */
  public void deleteHistoryEntry(String storedName) {
    jsonHistoryRepository.deleteByStoredName(storedName);
    if (currentView != null
        && currentView.historyEntry() != null
        && storedName.equals(currentView.historyEntry().storedName())) {
      replaceCurrentView(null);
    }
  }

  /**
   * Returns the current viewer state when one is loaded.
   *
   * @return current viewer state if available.
   */
  public Optional<JsonViewerLoadResult> currentView() {
    if (currentView == null || !currentView.hasLargePreviewSession()) {
      return Optional.ofNullable(currentView);
    }
    LargePreviewPagedSession refreshedSession =
        largePreviewSessionService
            .session(currentView.largePreviewSession().sessionId())
            .orElse(currentView.largePreviewSession());
    currentView = currentView.withLargePreviewSession(refreshedSession);
    return Optional.of(currentView);
  }

  /**
   * Returns raw JSON for the current view from history storage or source path.
   *
   * @return raw JSON content for the current view when it can be read.
   */
  public Optional<String> currentViewRawJson() {
    if (currentView == null) {
      return Optional.empty();
    }

    if (currentView.historyEntry() != null) {
      return jsonHistoryRepository.readStoredJson(currentView.historyEntry().storedName());
    }

    Path sourcePath = currentView.importResult().path();
    if (!Files.exists(sourcePath) || !Files.isReadable(sourcePath)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Files.readString(sourcePath));
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Unable to read current JSON source: " + sourcePath, exception);
    }
  }

  /** Opens history through the input port contract. */
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
        true,
        false);
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

  private String composeValidationMessage(JsonValidationResult validationResult) {
    if (validationResult.line() == null || validationResult.column() == null) {
      return validationResult.message();
    }
    return validationResult.message()
        + " (line "
        + validationResult.line()
        + ", column "
        + validationResult.column()
        + ")";
  }

  private AsciiTreeDocument renderDocument(Path path, JsonInspectionMode inspectionMode) {
    return inspectionMode == JsonInspectionMode.LARGE_PREVIEW
        ? asciiTreeRendererPort.renderLargePreview(path)
        : asciiTreeRendererPort.render(path);
  }

  private AsciiTreeDocument toAsciiTreeDocument(LargePreviewPageLoadResult pageLoadResult) {
    return new AsciiTreeDocument(
        "root",
        pageLoadResult.page().content(),
        pageLoadResult.page().descriptor().logicalLineCount());
  }

  private LargePreviewSessionSource sourceFor(
      JsonImportResult importResult, ImportedJsonFile historyEntry) {
    return switch (importResult.sourceKind()) {
      case LOCAL_FILE -> LargePreviewSessionSource.local(importResult.path());
      case CLIPBOARD -> LargePreviewSessionSource.clipboard(importResult.path());
      case HISTORY ->
          LargePreviewSessionSource.history(importResult.path(), historyEntry.storedName());
    };
  }

  private void replaceCurrentView(JsonViewerLoadResult nextView) {
    String previousSessionId =
        currentView != null && currentView.largePreviewSession() != null
            ? currentView.largePreviewSession().sessionId()
            : null;
    String nextSessionId =
        nextView != null && nextView.largePreviewSession() != null
            ? nextView.largePreviewSession().sessionId()
            : null;
    if (previousSessionId != null && !previousSessionId.equals(nextSessionId)) {
      largePreviewSessionService.closeSession(previousSessionId);
    }
    currentView = nextView;
  }

  private JsonViewerCapabilities capabilitiesFor(JsonInspectionMode inspectionMode) {
    return inspectionMode == JsonInspectionMode.LARGE_PREVIEW
        ? JsonViewerCapabilities.largePreview()
        : JsonViewerCapabilities.full();
  }
}
