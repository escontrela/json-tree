package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;

public record HistoryFavoritesViewState(
    List<ImportedJsonFile> visibleEntries,
    String summaryLabel,
    String emptyMessage,
    String toggleButtonText,
    boolean favoritesOnly) {

  public HistoryFavoritesViewState {
    visibleEntries = List.copyOf(visibleEntries);
  }
}
