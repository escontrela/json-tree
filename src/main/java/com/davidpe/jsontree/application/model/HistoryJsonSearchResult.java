package com.davidpe.jsontree.application.model;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;

public record HistoryJsonSearchResult(
    HistoryJsonSearchStatus status,
    String query,
    List<ImportedJsonFile> entries
) {

  public HistoryJsonSearchResult {
    query = query == null ? "" : query;
    entries = List.copyOf(entries);
  }

  public static HistoryJsonSearchResult cleared(List<ImportedJsonFile> entries) {
    return new HistoryJsonSearchResult(HistoryJsonSearchStatus.CLEARED, "", entries);
  }

  public static HistoryJsonSearchResult matches(String query, List<ImportedJsonFile> entries) {
    return new HistoryJsonSearchResult(HistoryJsonSearchStatus.MATCHES, query, entries);
  }

  public static HistoryJsonSearchResult noResults(String query) {
    return new HistoryJsonSearchResult(HistoryJsonSearchStatus.NO_RESULTS, query, List.of());
  }

  public static HistoryJsonSearchResult blocked(List<ImportedJsonFile> entries) {
    return new HistoryJsonSearchResult(HistoryJsonSearchStatus.BLOCKED, "", entries);
  }

  public boolean searchActive() {
    return status == HistoryJsonSearchStatus.MATCHES || status == HistoryJsonSearchStatus.NO_RESULTS;
  }

  public boolean blocked() {
    return status == HistoryJsonSearchStatus.BLOCKED;
  }
}
