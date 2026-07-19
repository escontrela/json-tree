package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.CurlDocumentImportResult;
import com.davidpe.jsontree.application.model.CurlDocumentImportStatus;
import com.davidpe.jsontree.application.model.CurlExecutionRequest;
import com.davidpe.jsontree.application.model.CurlExecutionResult;
import com.davidpe.jsontree.application.port.out.CurlRequestExecutorPort;
import com.davidpe.jsontree.domain.model.DocumentFormat;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Executes supported curl requests and materializes supported responses into the viewer workflow.
 */
@Service
public class CurlDocumentImportService {

  private static final DateTimeFormatter MATERIALIZED_FILE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
          .withLocale(Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  private final CurlRequestExecutorPort curlRequestExecutorPort;
  private final JsonViewerWorkflowService workflowService;
  private final Clock clock;
  private final Path tempRootDirectory;

  @Autowired
  public CurlDocumentImportService(
      CurlRequestExecutorPort curlRequestExecutorPort, JsonViewerWorkflowService workflowService) {
    this(
        curlRequestExecutorPort,
        workflowService,
        Clock.systemDefaultZone(),
        Path.of(System.getProperty("java.io.tmpdir")));
  }

  CurlDocumentImportService(
      CurlRequestExecutorPort curlRequestExecutorPort,
      JsonViewerWorkflowService workflowService,
      Clock clock,
      Path tempRootDirectory) {
    this.curlRequestExecutorPort = curlRequestExecutorPort;
    this.workflowService = workflowService;
    this.clock = clock;
    this.tempRootDirectory = tempRootDirectory;
  }

  /**
   * Executes the provided normalized request and imports the supported response into the normal
   * document workflow.
   */
  public CurlDocumentImportResult importRequest(CurlExecutionRequest request) {
    CurlExecutionResult executionResult = curlRequestExecutorPort.execute(request);
    if (!executionResult.successful()) {
      return CurlDocumentImportResult.failure(
          CurlDocumentImportStatus.EXECUTION_FAILED,
          appendAttemptedRequest(executionResult.failureMessage(), request),
          executionResult.statusCode() > 0 ? executionResult.statusCode() : null);
    }
    if (executionResult.statusCode() >= 400) {
      return CurlDocumentImportResult.failure(
          CurlDocumentImportStatus.EXECUTION_FAILED,
          appendAttemptedRequest(composeHttpFailureMessage(executionResult.statusCode()), request),
          executionResult.statusCode());
    }

    DocumentFormat documentFormat = detectDocumentFormat(executionResult);
    if (documentFormat == null) {
      return CurlDocumentImportResult.failure(
          CurlDocumentImportStatus.UNSUPPORTED_RESPONSE,
          appendAttemptedRequest(
              "HTTP "
                  + executionResult.statusCode()
                  + " returned content that is not a supported JSON or Markdown document.",
              request),
          executionResult.statusCode());
    }

    Path materializedPath = materializeResponse(request, executionResult, documentFormat);
    JsonImportResult importResult =
        new JsonImportResult(
            materializedPath,
            materializedPath.getFileName().toString(),
            resolveSize(materializedPath),
            true,
            true,
            true,
            JsonDocumentSourceKind.CURL,
            documentFormat);
    return CurlDocumentImportResult.imported(
        workflowService.loadImportedFile(importResult, request.rawCommand()));
  }

  private String composeHttpFailureMessage(int statusCode) {
    return switch (statusCode) {
      case 401 -> "HTTP 401: remote endpoint requires authentication.";
      case 403 -> "HTTP 403: access denied by the remote endpoint.";
      case 404 -> "HTTP 404: remote resource not found.";
      case 429 -> "HTTP 429: remote endpoint rate-limited the request.";
      default -> "HTTP " + statusCode + ": curl fetch failed.";
    };
  }

  private String appendAttemptedRequest(String message, CurlExecutionRequest request) {
    return message + " Request: " + request.rawCommand();
  }

  private DocumentFormat detectDocumentFormat(CurlExecutionResult executionResult) {
    String contentType = executionResult.contentType().toLowerCase(Locale.ROOT);
    if (contentType.contains("json") || contentType.contains("+json")) {
      return DocumentFormat.JSON;
    }
    if (contentType.contains("markdown")) {
      return DocumentFormat.MARKDOWN;
    }

    String body = decodeBody(executionResult);
    String trimmed = body.stripLeading();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      return DocumentFormat.JSON;
    }
    if (looksLikeMarkdown(body)) {
      return DocumentFormat.MARKDOWN;
    }
    return null;
  }

  private boolean looksLikeMarkdown(String body) {
    List<String> lines = body.lines().limit(12).toList();
    return lines.stream().anyMatch(this::markdownSignal);
  }

  private boolean markdownSignal(String line) {
    String trimmed = line.stripLeading();
    return trimmed.startsWith("#")
        || trimmed.startsWith("> ")
        || trimmed.startsWith("- ")
        || trimmed.startsWith("* ")
        || trimmed.startsWith("```");
  }

  private Path materializeResponse(
      CurlExecutionRequest request,
      CurlExecutionResult executionResult,
      DocumentFormat documentFormat) {
    try {
      Path curlDirectory = tempRootDirectory.resolve("json-tree-curl");
      Files.createDirectories(curlDirectory);
      Path materializedPath =
          nextMaterializedPath(
              curlDirectory, baseNameFor(request.url(), documentFormat), extensionFor(documentFormat));
      Files.writeString(materializedPath, decodeBody(executionResult));
      materializedPath.toFile().deleteOnExit();
      return materializedPath.toAbsolutePath().normalize();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to materialize curl response.", exception);
    }
  }

  private String decodeBody(CurlExecutionResult executionResult) {
    Charset charset =
        executionResult.charsetName().isBlank()
            ? StandardCharsets.UTF_8
            : Charset.forName(executionResult.charsetName());
    return new String(executionResult.responseBody(), charset);
  }

  private Path nextMaterializedPath(Path directory, String baseName, String extension) {
    String timestamp = MATERIALIZED_FILE_FORMATTER.format(Instant.now(clock));
    Path candidate = directory.resolve(baseName + "-" + timestamp + extension);
    int suffix = 2;
    while (Files.exists(candidate)) {
      candidate = directory.resolve(baseName + "-" + timestamp + "-" + suffix + extension);
      suffix++;
    }
    return candidate;
  }

  private String baseNameFor(URI uri, DocumentFormat documentFormat) {
    String path = uri.getPath() == null ? "" : uri.getPath().trim();
    String candidate = path.isBlank() ? uri.getHost() : Path.of(path).getFileName().toString();
    candidate = candidate == null || candidate.isBlank() ? "curl-response" : candidate;
    int extensionIndex = candidate.lastIndexOf('.');
    if (extensionIndex > 0) {
      candidate = candidate.substring(0, extensionIndex);
    }
    candidate = candidate.replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-{2,}", "-");
    if (candidate.isBlank()) {
      candidate = documentFormat == DocumentFormat.MARKDOWN ? "curl-markdown" : "curl-json";
    }
    return candidate;
  }

  private String extensionFor(DocumentFormat documentFormat) {
    return documentFormat == DocumentFormat.MARKDOWN ? ".md" : ".json";
  }

  private long resolveSize(Path path) {
    try {
      return Files.size(path);
    } catch (IOException exception) {
      return 0L;
    }
  }
}
