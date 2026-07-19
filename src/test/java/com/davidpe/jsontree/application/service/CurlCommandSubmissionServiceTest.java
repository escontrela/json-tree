package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.CurlCommandSource;
import com.davidpe.jsontree.application.model.CurlDocumentImportResult;
import com.davidpe.jsontree.application.model.CurlDocumentImportStatus;
import com.davidpe.jsontree.application.model.CurlExecutionRequest;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CurlCommandSubmissionServiceTest {

  @Test
  void rejectsEmptyOrNonCurlTextAsInvalidManualSubmission() {
    CurlCommandSubmissionService service =
        new CurlCommandSubmissionService(
            new CurlCommandParserService(), new RecordingCurlDocumentImportService());

    CurlDocumentImportResult result = service.submitCurlCommand("not a curl");

    assertEquals(CurlDocumentImportStatus.INVALID_CURL, result.status());
    assertEquals("Enter one supported curl command to run.", result.message());
  }

  @Test
  void returnsReadableParserValidationErrors() {
    CurlCommandSubmissionService service =
        new CurlCommandSubmissionService(
            new CurlCommandParserService(), new RecordingCurlDocumentImportService());

    CurlDocumentImportResult result =
        service.submitCurlCommand("curl https://example.com/items | jq .");

    assertEquals(CurlDocumentImportStatus.INVALID_CURL, result.status());
    assertTrue(result.message().contains("Unsupported shell features"));
  }

  @Test
  void forwardsSuccessfulManualCurlRequestsIntoSharedImportWorkflow() {
    RecordingCurlDocumentImportService importService = new RecordingCurlDocumentImportService();
    CurlCommandSubmissionService service =
        new CurlCommandSubmissionService(new CurlCommandParserService(), importService);

    CurlDocumentImportResult result =
        service.submitCurlCommand("curl --location https://example.com/items");

    assertTrue(result.successful());
    assertEquals("curl --location https://example.com/items", importService.lastRequest().rawCommand());
    assertEquals(CurlCommandSource.editor(), importService.lastRequest().source());
  }

  private static final class RecordingCurlDocumentImportService extends CurlDocumentImportService {

    private final AtomicReference<CurlExecutionRequest> lastRequest = new AtomicReference<>();

    private RecordingCurlDocumentImportService() {
      super(request -> {
        throw new AssertionError("Transport should not run in this unit test.");
      }, null);
    }

    @Override
    public CurlDocumentImportResult importRequest(CurlExecutionRequest request) {
      lastRequest.set(request);
      return CurlDocumentImportResult.imported((JsonViewerLoadResult) null);
    }

    private CurlExecutionRequest lastRequest() {
      return lastRequest.get();
    }
  }
}
