package com.davidpe.jsontree.ui.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Normalizes highlight ranges so downstream renderers consume non-overlapping segments.
 *
 * <p>When ranges overlap, active matches take precedence over inactive ones. Adjacent ranges with
 * the same active state are merged to keep the render model compact.
 */
@Component
public class SearchHighlightRangeNormalizer {

  public List<SearchHighlightRange> normalize(List<SearchHighlightRange> ranges) {
    if (ranges == null || ranges.isEmpty()) {
      return List.of();
    }

    List<Integer> boundaries =
        ranges.stream()
            .flatMap(range -> java.util.stream.Stream.of(range.startIndex(), range.endIndex()))
            .distinct()
            .sorted()
            .toList();

    if (boundaries.size() < 2) {
      return List.of();
    }

    List<SearchHighlightRange> normalized = new ArrayList<>();
    for (int index = 0; index < boundaries.size() - 1; index++) {
      int start = boundaries.get(index);
      int end = boundaries.get(index + 1);
      if (end <= start) {
        continue;
      }

      boolean covered = false;
      boolean active = false;
      for (SearchHighlightRange range : ranges) {
        if (range.startIndex() < end && range.endIndex() > start) {
          covered = true;
          active = active || range.active();
        }
      }

      if (!covered) {
        continue;
      }

      if (!normalized.isEmpty()) {
        SearchHighlightRange previous = normalized.getLast();
        if (previous.endIndex() == start && previous.active() == active) {
          normalized.set(
              normalized.size() - 1,
              new SearchHighlightRange(previous.startIndex(), end, previous.active()));
          continue;
        }
      }

      normalized.add(new SearchHighlightRange(start, end, active));
    }

    return normalized.stream()
        .sorted(Comparator.comparingInt(SearchHighlightRange::startIndex))
        .toList();
  }
}
