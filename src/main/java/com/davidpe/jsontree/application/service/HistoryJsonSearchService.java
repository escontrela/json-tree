package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.HistoryJsonSearchResult;
import com.davidpe.jsontree.application.port.in.SearchHistoryJsonUseCase;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class HistoryJsonSearchService implements SearchHistoryJsonUseCase {

  private final JsonHistoryRepository jsonHistoryRepository;

  public HistoryJsonSearchService(JsonHistoryRepository jsonHistoryRepository) {
    this.jsonHistoryRepository = jsonHistoryRepository;
  }

  @Override
  public HistoryJsonSearchResult search(String rawQuery, boolean searchAllowed) {
    List<ImportedJsonFile> allEntries = jsonHistoryRepository.findAll();
    if (!searchAllowed) {
      return HistoryJsonSearchResult.blocked(allEntries);
    }

    String query = rawQuery == null ? "" : rawQuery.trim();
    if (query.isBlank()) {
      return HistoryJsonSearchResult.cleared(allEntries);
    }

    String normalizedQuery = query.toLowerCase(Locale.ROOT);
    List<ImportedJsonFile> matchingEntries =
        allEntries.stream()
            .filter(entry -> matchesStoredJson(entry, normalizedQuery))
            .toList();

    if (matchingEntries.isEmpty()) {
      return HistoryJsonSearchResult.noResults(query);
    }
    return HistoryJsonSearchResult.matches(query, matchingEntries);
  }

  private boolean matchesStoredJson(ImportedJsonFile entry, String normalizedQuery) {
    return jsonHistoryRepository
        .readStoredJson(entry.storedName())
        .map(json -> json.toLowerCase(Locale.ROOT).contains(normalizedQuery))
        .orElse(false);
  }
}
