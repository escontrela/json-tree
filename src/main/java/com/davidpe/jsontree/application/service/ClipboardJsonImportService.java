package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.ClipboardJsonImportResult;
import com.davidpe.jsontree.application.model.ClipboardJsonImportStatus;
import com.davidpe.jsontree.application.port.in.ImportClipboardJsonUseCase;
import com.davidpe.jsontree.application.port.out.ClipboardPort;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClipboardJsonImportService implements ImportClipboardJsonUseCase {

  private static final DateTimeFormatter CLIPBOARD_FILE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
          .withLocale(Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  private final ClipboardPort clipboardPort;
  private final JsonViewerWorkflowService workflowService;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Path tempRootDirectory;

  @Autowired
  public ClipboardJsonImportService(
      ClipboardPort clipboardPort,
      JsonViewerWorkflowService workflowService,
      ObjectMapper objectMapper
  ) {
    this(
        clipboardPort,
        workflowService,
        objectMapper,
        Clock.systemDefaultZone(),
        Path.of(System.getProperty("java.io.tmpdir")));
  }

  ClipboardJsonImportService(
      ClipboardPort clipboardPort,
      JsonViewerWorkflowService workflowService,
      ObjectMapper objectMapper,
      Clock clock,
      Path tempRootDirectory
  ) {
    this.clipboardPort = clipboardPort;
    this.workflowService = workflowService;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.tempRootDirectory = tempRootDirectory;
  }

  @Override
  public ClipboardJsonImportResult importFromClipboard() {
    Optional<String> clipboardText;
    try {
      clipboardText = clipboardPort.readText();
    } catch (RuntimeException exception) {
      return ClipboardJsonImportResult.failure(
          ClipboardJsonImportStatus.UNREADABLE_CLIPBOARD,
          "Clipboard text is not available right now.");
    }

    if (clipboardText.isEmpty() || clipboardText.get().isBlank()) {
      return ClipboardJsonImportResult.failure(
          ClipboardJsonImportStatus.EMPTY_CLIPBOARD,
          "Clipboard does not contain JSON text.");
    }

    String rawJson = clipboardText.get();
    try {
      objectMapper.readTree(rawJson);
    } catch (JsonProcessingException exception) {
      return ClipboardJsonImportResult.failure(
          ClipboardJsonImportStatus.INVALID_JSON,
          composeInvalidJsonMessage(exception));
    }

    Path materializedJson = materializeClipboardJson(rawJson);
    JsonImportResult importResult =
        workflowService.importFile(materializedJson);
    return ClipboardJsonImportResult.success(workflowService.loadImportedFile(importResult));
  }

  private Path materializeClipboardJson(String rawJson) {
    try {
      Path clipboardDirectory = tempRootDirectory.resolve("json-tree-clipboard");
      Files.createDirectories(clipboardDirectory);

      Path materializedPath = nextMaterializedPath(clipboardDirectory);
      Files.writeString(materializedPath, rawJson);
      materializedPath.toFile().deleteOnExit();
      return materializedPath;
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to materialize clipboard JSON.", exception);
    }
  }

  private Path nextMaterializedPath(Path clipboardDirectory) {
    String baseName = "clipboard-" + CLIPBOARD_FILE_FORMATTER.format(Instant.now(clock));
    Path candidate = clipboardDirectory.resolve(baseName + ".json");
    int suffix = 2;
    while (Files.exists(candidate)) {
      candidate = clipboardDirectory.resolve(baseName + "-" + suffix + ".json");
      suffix++;
    }
    return candidate;
  }

  private String composeInvalidJsonMessage(JsonProcessingException exception) {
    if (exception.getLocation() == null) {
      return "Clipboard text is not valid JSON.";
    }
    return "Clipboard text is not valid JSON (line "
        + exception.getLocation().getLineNr()
        + ", column "
        + exception.getLocation().getColumnNr()
        + ").";
  }
}
