package com.davidpe.jsontree.application.model;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of materializing a large-preview session into temporary ASCII pages.
 *
 * <p>The snapshot captures persisted page descriptors, aggregate logical line counts, and the
 * compact outline digest produced during the same streaming pass.
 */
public record LargePreviewMaterializationSnapshot(
    String sessionId,
    Path sessionStoragePath,
    List<LargePreviewPageDescriptor> pages,
    long totalLogicalLines,
    LargePreviewOutlineDigest outlineDigest) {

  public LargePreviewMaterializationSnapshot {
    if (sessionId == null || sessionId.isBlank()) {
      throw new IllegalArgumentException("Large-preview materialization session id is required.");
    }
    if (sessionStoragePath == null) {
      throw new IllegalArgumentException(
          "Large-preview materialization storage path is required.");
    }
    if (totalLogicalLines < 0L) {
      throw new IllegalArgumentException(
          "Large-preview materialization total logical lines must be zero or greater.");
    }
    outlineDigest = outlineDigest == null ? LargePreviewOutlineDigest.empty() : outlineDigest;
    pages =
        Objects.requireNonNullElse(pages, List.<LargePreviewPageDescriptor>of()).stream()
            .sorted(Comparator.comparingInt(LargePreviewPageDescriptor::pageIndex))
            .toList();
  }

  public int totalPages() {
    return pages.size();
  }

  public List<LargePreviewPageRange> pageRanges() {
    return pages.stream().map(LargePreviewPageRange::fromDescriptor).toList();
  }
}
