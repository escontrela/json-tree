package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.CurlCommandParseResult;
import com.davidpe.jsontree.application.model.CurlCommandParseStatus;
import com.davidpe.jsontree.application.model.CurlCommandSource;
import com.davidpe.jsontree.application.model.CurlDocumentImportResult;
import com.davidpe.jsontree.application.model.CurlDocumentImportStatus;
import com.davidpe.jsontree.application.port.in.SubmitCurlCommandUseCase;
import org.springframework.stereotype.Service;

/**
 * Routes manually submitted curl text through the existing parser, transport, and materialization
 * workflow used by clipboard and dropped-file imports.
 */
@Service
public class CurlCommandSubmissionService implements SubmitCurlCommandUseCase {

  private final CurlCommandParserService curlCommandParserService;
  private final CurlDocumentImportService curlDocumentImportService;

  public CurlCommandSubmissionService(
      CurlCommandParserService curlCommandParserService,
      CurlDocumentImportService curlDocumentImportService) {
    this.curlCommandParserService = curlCommandParserService;
    this.curlDocumentImportService = curlDocumentImportService;
  }

  @Override
  public CurlDocumentImportResult submitCurlCommand(String rawCommand) {
    CurlCommandParseResult parseResult =
        curlCommandParserService.detectAndParse(rawCommand, CurlCommandSource.editor());
    if (parseResult.successful()) {
      return curlDocumentImportService.importRequest(parseResult.request());
    }
    if (parseResult.status() == CurlCommandParseStatus.INVALID) {
      return CurlDocumentImportResult.failure(
          CurlDocumentImportStatus.INVALID_CURL, parseResult.message());
    }
    return CurlDocumentImportResult.failure(
        CurlDocumentImportStatus.INVALID_CURL,
        "Enter one supported curl command to run.");
  }
}
