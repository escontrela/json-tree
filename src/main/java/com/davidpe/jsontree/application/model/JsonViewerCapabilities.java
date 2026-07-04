package com.davidpe.jsontree.application.model;

public record JsonViewerCapabilities(
    boolean rawJsonAvailable,
    boolean searchAvailable,
    boolean outlineAvailable) {

  public static JsonViewerCapabilities full() {
    return new JsonViewerCapabilities(true, true, true);
  }

  public static JsonViewerCapabilities largePreview() {
    return new JsonViewerCapabilities(false, false, false);
  }
}
