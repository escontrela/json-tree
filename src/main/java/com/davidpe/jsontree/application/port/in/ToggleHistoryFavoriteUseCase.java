package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.application.model.HistoryFavoriteToggleResult;

public interface ToggleHistoryFavoriteUseCase {

  HistoryFavoriteToggleResult toggleFavorite(String storedName);
}
