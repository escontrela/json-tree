package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.application.model.HistoryJsonSearchResult;

public interface SearchHistoryJsonUseCase {

  HistoryJsonSearchResult search(String rawQuery, boolean searchAllowed);
}
