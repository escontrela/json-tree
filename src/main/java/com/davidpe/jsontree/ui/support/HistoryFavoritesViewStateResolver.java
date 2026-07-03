package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HistoryFavoritesViewStateResolver {

  public HistoryFavoritesViewState resolve(List<ImportedJsonFile> allEntries, boolean favoritesOnly) {
    List<ImportedJsonFile> visibleEntries =
        favoritesOnly
            ? allEntries.stream().filter(ImportedJsonFile::favorite).toList()
            : List.copyOf(allEntries);

    String summaryLabel =
        favoritesOnly
            ? visibleEntries.size() + " favorite snapshot" + (visibleEntries.size() == 1 ? "" : "s")
            : allEntries.size() + " stored snapshot" + (allEntries.size() == 1 ? "" : "s");
    String footerLabel =
        favoritesOnly
            ? "Browsing pinned JSON favorites"
            : (allEntries.isEmpty() ? "No snapshots stored yet" : "Browsing local JSON history");
    String emptyMessage =
        favoritesOnly
            ? "No pinned JSON snapshots yet.\nPin entries from the list to build a focused favorites view."
            : "No JSON snapshots yet.\nDrop a valid JSON in the main view to start building history.";
    String toggleButtonText = favoritesOnly ? "Show all" : "Favorites only";

    return new HistoryFavoritesViewState(
        visibleEntries,
        summaryLabel,
        footerLabel,
        emptyMessage,
        toggleButtonText,
        favoritesOnly);
  }
}
