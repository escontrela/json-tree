package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class HistoryFavoritePresentationResolverTest {

  private final HistoryFavoritePresentationResolver resolver =
      new HistoryFavoritePresentationResolver();

  @Test
  void resolvesPinnedStateWithStarredTitleAndPinnedLabel() {
    HistoryFavoritePresentation presentation = resolver.resolve(sample(true));

    assertEquals("★ sample.json", presentation.title());
    assertEquals("Pinned", presentation.buttonText());
    assertTrue(presentation.active());
  }

  @Test
  void resolvesUnpinnedStateWithPlainTitleAndPinLabel() {
    HistoryFavoritePresentation presentation = resolver.resolve(sample(false));

    assertEquals("sample.json", presentation.title());
    assertEquals("Pin", presentation.buttonText());
    assertFalse(presentation.active());
  }

  private ImportedJsonFile sample(boolean favorite) {
    return new ImportedJsonFile(
        "2026-07-03_00-20-00_sample.json",
        "sample.json",
        Instant.parse("2026-07-03T00:20:00Z"),
        20L,
        4,
        true,
        favorite);
  }
}
