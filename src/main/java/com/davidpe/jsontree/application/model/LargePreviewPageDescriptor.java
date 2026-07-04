package com.davidpe.jsontree.application.model;

import java.nio.file.Path;

public record LargePreviewPageDescriptor(
    int pageIndex, Path storagePath, long startingLogicalLine, int logicalLineCount) {

  public LargePreviewPageDescriptor {
    if (pageIndex < 0) {
      throw new IllegalArgumentException("Large-preview page index must be zero or greater.");
    }
    if (storagePath == null) {
      throw new IllegalArgumentException("Large-preview page storage path is required.");
    }
    if (startingLogicalLine < 0L) {
      throw new IllegalArgumentException(
          "Large-preview page starting logical line must be zero or greater.");
    }
    if (logicalLineCount < 0) {
      throw new IllegalArgumentException(
          "Large-preview page logical line count must be zero or greater.");
    }
  }
}
