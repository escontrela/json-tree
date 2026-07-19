package com.davidpe.jsontree.application.model;

/**
 * Parse result for clipboard or dropped-file curl detection.
 */
public record CurlCommandParseResult(
    CurlCommandParseStatus status, String message, CurlExecutionRequest request) {

  public static CurlCommandParseResult success(CurlExecutionRequest request) {
    return new CurlCommandParseResult(CurlCommandParseStatus.SUCCESS, "", request);
  }

  public static CurlCommandParseResult notCurl() {
    return new CurlCommandParseResult(CurlCommandParseStatus.NOT_CURL, "", null);
  }

  public static CurlCommandParseResult invalid(String message) {
    return new CurlCommandParseResult(CurlCommandParseStatus.INVALID, message, null);
  }

  public boolean successful() {
    return status == CurlCommandParseStatus.SUCCESS;
  }
}
