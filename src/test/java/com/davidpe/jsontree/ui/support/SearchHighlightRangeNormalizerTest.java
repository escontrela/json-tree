package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SearchHighlightRangeNormalizerTest {

  private final SearchHighlightRangeNormalizer normalizer = new SearchHighlightRangeNormalizer();

  @Test
  void mergesOverlappingRangesAndPromotesActiveMatches() {
    List<SearchHighlightRange> normalized =
        normalizer.normalize(
            List.of(
                new SearchHighlightRange(4, 10, false),
                new SearchHighlightRange(7, 13, true),
                new SearchHighlightRange(13, 15, true)));

    assertEquals(
        List.of(
            new SearchHighlightRange(4, 7, false),
            new SearchHighlightRange(7, 15, true)),
        normalized);
  }
}
