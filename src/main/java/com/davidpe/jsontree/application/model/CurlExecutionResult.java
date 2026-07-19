package com.davidpe.jsontree.application.model;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Transport-level result of executing a normalized curl request.
 */
public record CurlExecutionResult(
    boolean successful,
    int statusCode,
    URI effectiveUri,
    Map<String, List<String>> responseHeaders,
    byte[] responseBody,
    String contentType,
    String charsetName,
    String failureMessage) {

  public CurlExecutionResult {
    responseHeaders = responseHeaders == null ? Map.of() : Map.copyOf(responseHeaders);
    responseBody = responseBody == null ? new byte[0] : responseBody.clone();
    contentType = contentType == null ? "" : contentType;
    charsetName = charsetName == null ? "" : charsetName;
    failureMessage = failureMessage == null ? "" : failureMessage;
  }

  public static CurlExecutionResult success(
      int statusCode,
      URI effectiveUri,
      Map<String, List<String>> responseHeaders,
      byte[] responseBody,
      String contentType,
      String charsetName) {
    return new CurlExecutionResult(
        true, statusCode, effectiveUri, responseHeaders, responseBody, contentType, charsetName, "");
  }

  public static CurlExecutionResult failure(String message) {
    return new CurlExecutionResult(false, -1, null, Map.of(), new byte[0], "", "", message);
  }
}
