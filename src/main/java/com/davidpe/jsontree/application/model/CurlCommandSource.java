package com.davidpe.jsontree.application.model;

import java.nio.file.Path;

/**
 * Describes where a curl command came from before execution.
 */
public record CurlCommandSource(String label) {

  public static CurlCommandSource clipboard() {
    return new CurlCommandSource("Clipboard");
  }

  public static CurlCommandSource droppedFile(Path path) {
    return new CurlCommandSource("Dropped file: " + path.getFileName());
  }

  public static CurlCommandSource editor() {
    return new CurlCommandSource("Curl editor");
  }
}
