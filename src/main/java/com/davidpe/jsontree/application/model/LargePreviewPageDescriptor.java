package com.davidpe.jsontree.application.model;

import java.nio.file.Path;

/**
 * Describes one byte-bounded large-preview chunk.
 *
 * <p>The legacy field names are kept for compatibility with the current codebase, but in the
 * byte-paginated large-preview workflow they represent source-byte coordinates instead of logical
 * line coordinates.
 */
public record LargePreviewPageDescriptor(
    int pageIndex,
    Path storagePath,
    long startingLogicalLine,
    int logicalLineCount,
    int leadingOverlapBytes,
    int trailingOverlapBytes) {

  public LargePreviewPageDescriptor(
      int pageIndex, Path storagePath, long startingLogicalLine, int logicalLineCount) {
    this(pageIndex, storagePath, startingLogicalLine, logicalLineCount, 0, 0);
  }

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
    if (leadingOverlapBytes < 0 || trailingOverlapBytes < 0) {
      throw new IllegalArgumentException(
          "Large-preview chunk overlap bytes must be zero or greater.");
    }
    if (leadingOverlapBytes > logicalLineCount || trailingOverlapBytes > logicalLineCount) {
      throw new IllegalArgumentException(
          "Large-preview chunk overlap bytes must stay within the visible chunk size.");
    }
  }

  public long endingLogicalLineExclusive() {
    return startingLogicalLine + logicalLineCount;
  }
}
