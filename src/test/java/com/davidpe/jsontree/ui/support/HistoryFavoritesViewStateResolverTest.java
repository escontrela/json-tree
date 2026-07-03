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
    HistoryFavoritesViewState state =
        resolver.resolve(List.of(sample(false, "regular"), sample(true, "favorite")), false);

    assertEquals(2, state.visibleEntries().size());
    assertEquals("2 stored snapshots", state.summaryLabel());
    assertEquals("Favorites only", state.toggleButtonText());
    assertTrue(state.visibleEntries().getFirst().favorite());
  }

  @Test
  void resolvesOnlyFavoriteEntriesWhenFilterIsOn() {
    HistoryFavoritesViewState state =
        resolver.resolve(List.of(sample(false, "regular"), sample(true, "favorite")), true);

    assertEquals(1, state.visibleEntries().size());
    assertTrue(state.visibleEntries().getFirst().favorite());
    assertEquals("1 favorite snapshot", state.summaryLabel());
    assertEquals("Show all", state.toggleButtonText());
  }

  @Test
  void exposesFavoriteSpecificEmptyStateWhenNoPinnedEntriesExist() {
    HistoryFavoritesViewState state = resolver.resolve(List.of(sample(false, "regular")), true);

    assertTrue(state.visibleEntries().isEmpty());
    assertTrue(state.emptyMessage().contains("No pinned JSON snapshots yet."));
  }

  @Test
  void keepsChronologyInsideFavoriteAndRegularGroups() {
    ImportedJsonFile favoriteOlder = sample(true, "favorite-older", Instant.parse("2026-07-03T00:10:00Z"));
    ImportedJsonFile favoriteNewer = sample(true, "favorite-newer", Instant.parse("2026-07-03T00:20:00Z"));
    ImportedJsonFile regularOlder = sample(false, "regular-older", Instant.parse("2026-07-03T00:05:00Z"));
    ImportedJsonFile regularNewer = sample(false, "regular-newer", Instant.parse("2026-07-03T00:30:00Z"));

    HistoryFavoritesViewState state =
        resolver.resolve(List.of(regularNewer, favoriteNewer, regularOlder, favoriteOlder), false);

    assertEquals(
        List.of(favoriteOlder, favoriteNewer, regularOlder, regularNewer),
        state.visibleEntries());
  }

  @Test
  void deletingLastFavoriteLeavesFavoritesOnlyViewInSafeEmptyState() {
    ImportedJsonFile favorite = sample(true, "favorite");
    ImportedJsonFile regular = sample(false, "regular");

    HistoryFavoritesViewState beforeDelete =
        resolver.resolve(List.of(regular, favorite), true);
    HistoryFavoritesViewState afterDelete =
        resolver.resolve(List.of(regular), true);
    HistoryFavoritesViewState restoredAllEntries =
        resolver.resolve(List.of(regular), false);

    assertEquals(1, beforeDelete.visibleEntries().size());
    assertTrue(beforeDelete.visibleEntries().getFirst().favorite());

    assertTrue(afterDelete.visibleEntries().isEmpty());
    assertEquals("0 favorite snapshots", afterDelete.summaryLabel());
    assertEquals("Browsing pinned JSON favorites", afterDelete.footerLabel());
    assertTrue(afterDelete.emptyMessage().contains("No pinned JSON snapshots yet."));

    assertEquals(List.of(regular), restoredAllEntries.visibleEntries());
  }

  private ImportedJsonFile sample(boolean favorite, String name) {
    return sample(favorite, name, Instant.parse("2026-07-03T00:30:00Z"));
  }

  private ImportedJsonFile sample(boolean favorite, String name, Instant importedAt) {
    return new ImportedJsonFile(
        "2026-07-03_00-30-00_" + name + ".json",
        name + ".json",
        importedAt,
        20L,
        4,
        true,
        favorite);
  }
}
