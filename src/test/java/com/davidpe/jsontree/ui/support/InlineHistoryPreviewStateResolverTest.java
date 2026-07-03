package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InlineHistoryPreviewStateResolverTest {

  private final InlineHistoryPreviewStateResolver resolver = new InlineHistoryPreviewStateResolver();

  @Test
  void keepsAllEntriesWhenHistoryIsSmallerThanTheInlineCap() {
    ImportedJsonFile first = sample("first", "2026-07-03T00:10:00Z");
    ImportedJsonFile second = sample("second", "2026-07-03T00:20:00Z");

    InlineHistoryPreviewState state = resolver.resolve(List.of(first, second), 10);

    assertEquals(List.of(first, second), state.visibleEntries());
    assertEquals("2 recent snapshots", state.summaryLabel());
  }

  @Test
  void keepsOnlyTheTenMostRecentEntriesWhilePreservingChronology() {
    List<ImportedJsonFile> entries = new ArrayList<>();
    for (int index = 0; index < 12; index++) {
      entries.add(sample("entry-" + index, "2026-07-03T00:" + String.format("%02d", index) + ":00Z"));
    }

    InlineHistoryPreviewState state = resolver.resolve(entries, 10);

    assertEquals(entries.subList(2, 12), state.visibleEntries());
    assertEquals("10 recent snapshots", state.summaryLabel());
  }

  @Test
  void exposesEmptyStateWhenThereAreNoEntries() {
    InlineHistoryPreviewState state = resolver.resolve(List.of(), 10);

    assertTrue(state.visibleEntries().isEmpty());
    assertEquals("No recent snapshots", state.summaryLabel());
  }

  private ImportedJsonFile sample(String name, String importedAt) {
    return new ImportedJsonFile(
        importedAt.replace(":", "-").replace("T", "_").replace("Z", "") + "_" + name + ".json",
        name + ".json",
        Instant.parse(importedAt),
        20L,
        4,
        true,
        false);
  }
}
