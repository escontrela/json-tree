package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.application.model.CurlDocumentImportResult;

/**
 * Validates, executes, and materializes one manually submitted curl command through the shared
 * curl workflow.
 */
public interface SubmitCurlCommandUseCase {

  CurlDocumentImportResult submitCurlCommand(String rawCommand);
}
