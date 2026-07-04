package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HistoryFavoritesViewStateResolver {

  private static final Comparator<ImportedJsonFile> FAVORITES_FIRST_CHRONOLOGICAL =
      Comparator.comparing(ImportedJsonFile::favorite)
          .reversed()
          .thenComparing(ImportedJsonFile::importedAt);

  public HistoryFavoritesViewState resolve(
      List<ImportedJsonFile> allEntries, boolean favoritesOnly) {
    List<ImportedJsonFile> visibleEntries =
        favoritesOnly
            ? allEntries.stream()
                .filter(ImportedJsonFile::favorite)
                .sorted(FAVORITES_FIRST_CHRONOLOGICAL)
                .toList()
            : allEntries.stream().sorted(FAVORITES_FIRST_CHRONOLOGICAL).toList();

    String summaryLabel =
        favoritesOnly
            ? visibleEntries.size() + " favorite snapshot" + (visibleEntries.size() == 1 ? "" : "s")
            : allEntries.size() + " stored snapshot" + (allEntries.size() == 1 ? "" : "s");
    String emptyMessage =
        favoritesOnly
            ? "No pinned JSON snapshots yet.\n"
                  + "Pin entries from the list to build a focused favorites view."
            : "No JSON snapshots yet.\n"
                  + "Drop a valid JSON in the main view to start building history.";
    String toggleButtonText = favoritesOnly ? "Show all" : "Favorites only";

    return new HistoryFavoritesViewState(
        visibleEntries, summaryLabel, emptyMessage, toggleButtonText, favoritesOnly);
  }
}
