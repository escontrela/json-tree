package com.davidpe.jsontree.application.model;

import java.util.List;
import java.util.OptionalInt;

/**
 * Compact full-document outline for a paged large-preview session.
 *
 * <p>This digest keeps the lightweight metadata required by the existing minimap shell without
 * forcing the viewer to hold the full rendered ASCII document in memory.
 */
public record LargePreviewOutlineDigest(List<LargePreviewOutlineDigestEntry> entries, int maxDepth) {

  public LargePreviewOutlineDigest {
    entries = List.copyOf(entries);
    if (maxDepth < 0) {
      throw new IllegalArgumentException("Large-preview outline digest max depth must be non-negative.");
    }
  }

  public static LargePreviewOutlineDigest empty() {
    return new LargePreviewOutlineDigest(List.of(), 0);
  }

  public boolean emptyDigest() {
    return entries.isEmpty();
  }

  public OptionalInt pageIndexForEntry(int entryIndex) {
    if (entryIndex < 0 || entryIndex >= entries.size()) {
      return OptionalInt.empty();
    }
    return OptionalInt.of(entries.get(entryIndex).pageIndex());
  }
}
