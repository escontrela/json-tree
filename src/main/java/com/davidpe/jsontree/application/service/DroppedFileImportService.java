package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.CurlCommandParseResult;
import com.davidpe.jsontree.application.model.CurlCommandSource;
import com.davidpe.jsontree.application.model.CurlDocumentImportResult;
import com.davidpe.jsontree.application.model.DroppedFileImportResult;
import com.davidpe.jsontree.application.model.DroppedFileImportStatus;
import com.davidpe.jsontree.application.port.in.ImportDroppedFileUseCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Decides whether a dropped file should be opened directly or treated as a curl command container.
 */
@Service
public class DroppedFileImportService implements ImportDroppedFileUseCase {

  private final JsonViewerWorkflowService workflowService;
  private final CurlCommandParserService curlCommandParserService;
  private final CurlDocumentImportService curlDocumentImportService;

  @Autowired
  public DroppedFileImportService(
      JsonViewerWorkflowService workflowService,
      CurlCommandParserService curlCommandParserService,
      CurlDocumentImportService curlDocumentImportService) {
    this.workflowService = workflowService;
    this.curlCommandParserService = curlCommandParserService;
    this.curlDocumentImportService = curlDocumentImportService;
  }

  @Override
  public DroppedFileImportResult importDroppedFile(Path path) {
    if (path == null || !Files.exists(path) || !Files.isReadable(path) || !Files.isRegularFile(path)) {
      return DroppedFileImportResult.failure(
          DroppedFileImportStatus.UNREADABLE_FILE, "Dropped file is not readable.");
    }

    if (isNativeDocument(path)) {
      return DroppedFileImportResult.imported(workflowService.loadFile(path));
    }

    String contents;
    try {
      contents = Files.readString(path);
    } catch (IOException exception) {
      return DroppedFileImportResult.failure(
          DroppedFileImportStatus.UNREADABLE_FILE, "Dropped file could not be read.");
    }

    CurlCommandParseResult parseResult =
        curlCommandParserService.detectAndParse(contents, CurlCommandSource.droppedFile(path));
    if (parseResult.status() == com.davidpe.jsontree.application.model.CurlCommandParseStatus.NOT_CURL) {
      return DroppedFileImportResult.failure(
          DroppedFileImportStatus.UNSUPPORTED_DROP,
          "Dropped file is neither a supported local document nor a curl command file.");
    }
    if (!parseResult.successful()) {
      return DroppedFileImportResult.failure(
          DroppedFileImportStatus.INVALID_CURL, parseResult.message());
    }

    CurlDocumentImportResult importResult = curlDocumentImportService.importRequest(parseResult.request());
    if (importResult.successful()) {
      return DroppedFileImportResult.imported(importResult.loadResult());
    }
    return DroppedFileImportResult.failure(mapStatus(importResult.status()), importResult.message());
  }

  private boolean isNativeDocument(Path path) {
    String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
    return fileName.endsWith(".json") || fileName.endsWith(".md");
  }

  private DroppedFileImportStatus mapStatus(
      com.davidpe.jsontree.application.model.CurlDocumentImportStatus status) {
    return switch (status) {
      case INVALID_CURL -> DroppedFileImportStatus.INVALID_CURL;
      case EXECUTION_FAILED -> DroppedFileImportStatus.EXECUTION_FAILED;
      case UNSUPPORTED_RESPONSE -> DroppedFileImportStatus.UNSUPPORTED_RESPONSE;
      case UNREADABLE_SOURCE -> DroppedFileImportStatus.UNREADABLE_FILE;
      case IMPORTED -> DroppedFileImportStatus.IMPORTED;
    };
  }
}
