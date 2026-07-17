package com.davidpe.jsontree.application.model;

import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import java.nio.file.Path;

public record LargePreviewSessionSource(
    Path path, JsonDocumentSourceKind sourceKind, String storedSnapshotName) {

  public LargePreviewSessionSource {
    if (path == null) {
      throw new IllegalArgumentException("Large-preview session source path is required.");
    }
    path = path.toAbsolutePath().normalize();
    if (sourceKind == null) {
      throw new IllegalArgumentException("Large-preview session source kind is required.");
    }
    if (sourceKind == JsonDocumentSourceKind.HISTORY
        && (storedSnapshotName == null || storedSnapshotName.isBlank())) {
      throw new IllegalArgumentException(
          "History-backed large-preview sessions require the stored snapshot identity.");
    }
  }

  public static LargePreviewSessionSource local(Path path) {
    return new LargePreviewSessionSource(path, JsonDocumentSourceKind.LOCAL_FILE, null);
  }

  public static LargePreviewSessionSource clipboard(Path path) {
    return new LargePreviewSessionSource(path, JsonDocumentSourceKind.CLIPBOARD, null);
  }

  public static LargePreviewSessionSource history(Path path, String storedSnapshotName) {
    return new LargePreviewSessionSource(
        path, JsonDocumentSourceKind.HISTORY, storedSnapshotName);
  }
}
