package com.davidpe.jsontree.application.model;

public record JsonSearchExecutionResult(
    JsonSearchSession session,
    String errorMessage
) {

  public static JsonSearchExecutionResult success(JsonSearchSession session) {
    return new JsonSearchExecutionResult(session, null);
  }

  public static JsonSearchExecutionResult failure(String errorMessage) {
    return new JsonSearchExecutionResult(null, errorMessage);
  }

  public boolean successful() {
    return session != null && errorMessage == null;
  }
}
