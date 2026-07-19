package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.HistoryJsonSearchResult;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HistoryArchiveViewStateResolver {

  private final HistoryFavoritesViewStateResolver historyFavoritesViewStateResolver;

  public HistoryArchiveViewStateResolver(
      HistoryFavoritesViewStateResolver historyFavoritesViewStateResolver) {
    this.historyFavoritesViewStateResolver = historyFavoritesViewStateResolver;
  }

  public HistoryArchiveViewState resolve(
      List<ImportedJsonFile> allEntries,
      boolean favoritesOnly,
      HistoryJsonSearchResult searchResult
  ) {
    HistoryFavoritesViewState baseState =
        historyFavoritesViewStateResolver.resolve(allEntries, favoritesOnly);
    if (favoritesOnly || searchResult == null || !searchResult.searchActive()) {
      return new HistoryArchiveViewState(
          baseState.visibleEntries(),
          baseState.summaryLabel(),
          baseState.emptyMessage(),
          baseState.toggleButtonText(),
          baseState.favoritesOnly(),
          false);
    }

    List<ImportedJsonFile> visibleEntries =
        historyFavoritesViewStateResolver.resolve(searchResult.entries(), false).visibleEntries();
    String summaryLabel =
        visibleEntries.size()
            + " search result"
            + (visibleEntries.size() == 1 ? "" : "s")
            + " for \""
            + searchResult.query()
            + "\"";
    String emptyMessage =
        "No stored document matches \""
            + searchResult.query()
            + "\".\nClear the search field or try a different phrase.";

    return new HistoryArchiveViewState(
        visibleEntries,
        summaryLabel,
        emptyMessage,
        baseState.toggleButtonText(),
        baseState.favoritesOnly(),
        true);
  }
}
