package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;

public record HistoryArchiveViewState(
    List<ImportedJsonFile> visibleEntries,
    String summaryLabel,
    String emptyMessage,
    String toggleButtonText,
    boolean favoritesOnly,
    boolean searchActive
) {

  public HistoryArchiveViewState {
    visibleEntries = List.copyOf(visibleEntries);
  }
}
