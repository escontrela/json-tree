package com.davidpe.jsontree.application.model;

/**
 * One outline row inside a large-preview digest, including the page that should be targeted when
 * navigation jumps to this region.
 */
public record LargePreviewOutlineDigestEntry(int pageIndex, JsonOutlineEntry outlineEntry) {

  public LargePreviewOutlineDigestEntry {
    if (pageIndex < 0) {
      throw new IllegalArgumentException("Large-preview outline digest page index must be non-negative.");
    }
    if (outlineEntry == null) {
      throw new IllegalArgumentException("Large-preview outline digest entry is required.");
    }
  }
}
