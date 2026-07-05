package com.davidpe.jsontree.application.model;

/**
 * Document-wide source-byte range owned by one large-preview chunk.
 *
 * <p>The field names remain stable for compatibility, but in the current byte-paginated variant
 * they represent byte offsets and byte lengths.
 */
public record LargePreviewPageRange(int pageIndex, long startingLogicalLine, int logicalLineCount) {

  public LargePreviewPageRange {
    if (pageIndex < 0) {
      throw new IllegalArgumentException("Large-preview page range index must be zero or greater.");
    }
    if (startingLogicalLine < 0L) {
      throw new IllegalArgumentException(
          "Large-preview page range starting logical line must be zero or greater.");
    }
    if (logicalLineCount < 0) {
      throw new IllegalArgumentException(
          "Large-preview page range logical line count must be zero or greater.");
    }
  }

  public long endingLogicalLineExclusive() {
    return startingLogicalLine + logicalLineCount;
  }

  public boolean containsLogicalLine(long logicalLine) {
    return logicalLine >= startingLogicalLine && logicalLine < endingLogicalLineExclusive();
  }

  public static LargePreviewPageRange fromDescriptor(LargePreviewPageDescriptor descriptor) {
    if (descriptor == null) {
      throw new IllegalArgumentException("Large-preview page descriptor is required.");
    }
    return new LargePreviewPageRange(
        descriptor.pageIndex(), descriptor.startingLogicalLine(), descriptor.logicalLineCount());
  }
}
