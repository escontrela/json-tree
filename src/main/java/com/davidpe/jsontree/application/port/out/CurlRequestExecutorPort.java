package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.application.model.CurlExecutionRequest;
import com.davidpe.jsontree.application.model.CurlExecutionResult;

/**
 * Executes a normalized curl request without shelling out to the system curl binary.
 */
public interface CurlRequestExecutorPort {

  CurlExecutionResult execute(CurlExecutionRequest request);
}
