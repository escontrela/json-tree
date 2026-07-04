package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.HistoryFavoriteToggleResult;
import com.davidpe.jsontree.application.port.in.ToggleHistoryFavoriteUseCase;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class HistoryFavoriteService implements ToggleHistoryFavoriteUseCase {

  private final JsonHistoryRepository jsonHistoryRepository;

  public HistoryFavoriteService(JsonHistoryRepository jsonHistoryRepository) {
    this.jsonHistoryRepository = jsonHistoryRepository;
  }

  @Override
  public HistoryFavoriteToggleResult toggleFavorite(String storedName) {
    Optional<ImportedJsonFile> entry = jsonHistoryRepository.findByStoredName(storedName);
    if (entry.isEmpty()) {
      return HistoryFavoriteToggleResult.missing();
    }
    return jsonHistoryRepository
        .updateFavorite(storedName, !entry.get().favorite())
        .map(HistoryFavoriteToggleResult::updated)
        .orElseGet(HistoryFavoriteToggleResult::missing);
  }
}
