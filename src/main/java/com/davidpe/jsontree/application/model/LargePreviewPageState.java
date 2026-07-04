package com.davidpe.jsontree.application.model;

public record LargePreviewPageState(
    int pageIndex,
    LargePreviewPageStatus status,
    boolean residentInMemory,
    boolean persistedToTemporaryStorage,
    Integer logicalLineCount) {

  public LargePreviewPageState {
    if (pageIndex < 0) {
      throw new IllegalArgumentException("Large-preview page index must be zero or greater.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Large-preview page status is required.");
    }
    if (logicalLineCount != null && logicalLineCount < 0) {
      throw new IllegalArgumentException(
          "Large-preview page logical line count must be zero or greater when known.");
    }
    if (residentInMemory && !persistedToTemporaryStorage && status == LargePreviewPageStatus.AVAILABLE) {
      // Resident pages may still be persisted later, but available pages should already have a
      // stable recoverable representation before UI consumption relies on them.
      throw new IllegalArgumentException(
          "Available in-memory large-preview pages must also be recoverable from temporary storage.");
    }
  }

  public static LargePreviewPageState requested(int pageIndex) {
    return new LargePreviewPageState(pageIndex, LargePreviewPageStatus.REQUESTED, false, false, null);
  }

  public static LargePreviewPageState building(int pageIndex) {
    return new LargePreviewPageState(pageIndex, LargePreviewPageStatus.BUILDING, false, false, null);
  }

  public static LargePreviewPageState available(
      int pageIndex, boolean residentInMemory, boolean persistedToTemporaryStorage, int logicalLineCount) {
    return new LargePreviewPageState(
        pageIndex,
        LargePreviewPageStatus.AVAILABLE,
        residentInMemory,
        persistedToTemporaryStorage,
        logicalLineCount);
  }

  public static LargePreviewPageState failed(int pageIndex) {
    return new LargePreviewPageState(pageIndex, LargePreviewPageStatus.FAILED, false, false, null);
  }
}
