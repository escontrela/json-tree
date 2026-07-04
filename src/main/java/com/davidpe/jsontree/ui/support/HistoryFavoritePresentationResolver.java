package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import org.springframework.stereotype.Component;

@Component
public class HistoryFavoritePresentationResolver {

  public HistoryFavoritePresentation resolve(ImportedJsonFile entry) {
    if (entry.favorite()) {
      return new HistoryFavoritePresentation("★ " + entry.originalName(), "Pinned", true);
    }
    return new HistoryFavoritePresentation(entry.originalName(), "Pin", false);
  }
}
