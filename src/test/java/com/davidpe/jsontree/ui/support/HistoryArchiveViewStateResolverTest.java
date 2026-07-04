package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.HistoryJsonSearchResult;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryArchiveViewStateResolverTest {

  private final HistoryArchiveViewStateResolver resolver =
      new HistoryArchiveViewStateResolver(new HistoryFavoritesViewStateResolver());

  @Test
  void keepsFavoriteFilterStateWhenNoHistorySearchIsActive() {
    ImportedJsonFile favorite = entry("favorite", Instant.parse("2026-07-04T10:00:00Z"), true);
    ImportedJsonFile regular = entry("regular", Instant.parse("2026-07-04T11:00:00Z"), false);

    HistoryArchiveViewState state =
        resolver.resolve(
            List.of(regular, favorite),
            true,
            HistoryJsonSearchResult.cleared(List.of(regular, favorite)));

    assertTrue(state.favoritesOnly());
    assertFalse(state.searchActive());
    assertEquals(List.of(favorite), state.visibleEntries());
  }

  @Test
  void resolvesSearchSpecificSummaryAndEntriesWhenQueryMatchesHistory() {
    ImportedJsonFile favorite = entry("favorite", Instant.parse("2026-07-04T10:00:00Z"), true);
    ImportedJsonFile regular = entry("regular", Instant.parse("2026-07-04T11:00:00Z"), false);

    HistoryArchiveViewState state =
        resolver.resolve(
            List.of(regular, favorite),
            false,
            HistoryJsonSearchResult.matches("admin", List.of(regular, favorite)));

    assertTrue(state.searchActive());
    assertEquals("2 search results for \"admin\"", state.summaryLabel());
    assertEquals(List.of(favorite, regular), state.visibleEntries());
  }

  @Test
  void exposesDedicatedNoResultsMessagingForAnActiveHistorySearch() {
    ImportedJsonFile regular = entry("regular", Instant.parse("2026-07-04T11:00:00Z"), false);

    HistoryArchiveViewState state =
        resolver.resolve(
            List.of(regular),
            false,
            HistoryJsonSearchResult.noResults("missing"));

    assertTrue(state.searchActive());
    assertTrue(state.visibleEntries().isEmpty());
    assertEquals("0 search results for \"missing\"", state.summaryLabel());
    assertTrue(state.emptyMessage().contains("No stored JSON matches \"missing\"."));
  }

  @Test
  void clearedSearchRestoresTheStandardFullHistorySummary() {
    ImportedJsonFile regular = entry("regular", Instant.parse("2026-07-04T11:00:00Z"), false);

    HistoryArchiveViewState state =
        resolver.resolve(
            List.of(regular),
            false,
            HistoryJsonSearchResult.cleared(List.of(regular)));

    assertFalse(state.searchActive());
    assertEquals("1 stored snapshot", state.summaryLabel());
    assertEquals(List.of(regular), state.visibleEntries());
  }

  private ImportedJsonFile entry(String name, Instant importedAt, boolean favorite) {
    return new ImportedJsonFile(
        "2026-07-04_10-00-00_" + name + ".json",
        name + ".json",
        importedAt,
        20L,
        4,
        true,
        favorite);
  }
}
