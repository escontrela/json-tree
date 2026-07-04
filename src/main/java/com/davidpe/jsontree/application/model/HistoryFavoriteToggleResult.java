package com.davidpe.jsontree.application.model;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;

public record HistoryFavoriteToggleResult(
    HistoryFavoriteToggleStatus status,
    ImportedJsonFile entry
) {

  public HistoryFavoriteToggleResult {
    if ((status == HistoryFavoriteToggleStatus.MISSING) != (entry == null)) {
      throw new IllegalArgumentException(
          "Missing toggle results must not include an entry, and successful toggles must.");
    }
  }

  public static HistoryFavoriteToggleResult missing() {
    return new HistoryFavoriteToggleResult(HistoryFavoriteToggleStatus.MISSING, null);
  }

  public static HistoryFavoriteToggleResult updated(ImportedJsonFile entry) {
    return new HistoryFavoriteToggleResult(
        entry.favorite() ? HistoryFavoriteToggleStatus.FAVORITED : HistoryFavoriteToggleStatus.UNFAVORITED,
        entry);
  }

  public boolean found() {
    return status != HistoryFavoriteToggleStatus.MISSING;
  }
}
