package com.davidpe.jsontree.application.model;

import java.net.URI;
import java.util.List;

/**
 * Normalized subset of a supported curl command ready for transport execution.
 */
public record CurlExecutionRequest(
    String rawCommand,
    CurlCommandSource source,
    URI url,
    String method,
    boolean followRedirects,
    List<CurlExecutionHeader> headers,
    String body) {

  public CurlExecutionRequest {
    headers = headers == null ? List.of() : List.copyOf(headers);
    method = method == null ? "GET" : method.toUpperCase();
    body = body == null ? "" : body;
  }

  public boolean hasBody() {
    return !body.isEmpty();
  }
}
