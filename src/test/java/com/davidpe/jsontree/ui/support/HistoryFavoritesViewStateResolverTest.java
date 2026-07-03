package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryFavoritesViewStateResolverTest {

  private final HistoryFavoritesViewStateResolver resolver = new HistoryFavoritesViewStateResolver();

  @Test
  void resolvesAllEntriesViewWhenFavoritesFilterIsOff() {
    HistoryFavoritesViewState state = resolver.resolve(List.of(sample(false), sample(true)), false);

    assertEquals(2, state.visibleEntries().size());
    assertEquals("2 stored snapshots", state.summaryLabel());
    assertEquals("Favorites only", state.toggleButtonText());
  }

  @Test
  void resolvesOnlyFavoriteEntriesWhenFilterIsOn() {
    HistoryFavoritesViewState state = resolver.resolve(List.of(sample(false), sample(true)), true);

    assertEquals(1, state.visibleEntries().size());
    assertTrue(state.visibleEntries().getFirst().favorite());
    assertEquals("1 favorite snapshot", state.summaryLabel());
    assertEquals("Show all", state.toggleButtonText());
  }

  @Test
  void exposesFavoriteSpecificEmptyStateWhenNoPinnedEntriesExist() {
    HistoryFavoritesViewState state = resolver.resolve(List.of(sample(false)), true);

    assertTrue(state.visibleEntries().isEmpty());
    assertTrue(state.emptyMessage().contains("No pinned JSON snapshots yet."));
  }

  private ImportedJsonFile sample(boolean favorite) {
    return new ImportedJsonFile(
        "2026-07-03_00-30-00_sample.json",
        "sample.json",
        Instant.parse("2026-07-03T00:30:00Z"),
        20L,
        4,
        true,
        favorite);
  }
}
