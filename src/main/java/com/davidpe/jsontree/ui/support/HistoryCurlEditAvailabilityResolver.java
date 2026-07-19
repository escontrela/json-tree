package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import org.springframework.stereotype.Component;

/**
 * Determines whether a history entry can safely expose the curl edit/rerun affordance.
 */
@Component
public class HistoryCurlEditAvailabilityResolver {

  public boolean supports(ImportedJsonFile entry) {
    return entry != null
        && entry.curlBacked()
        && entry.curlCommand() != null
        && !entry.curlCommand().isBlank();
  }
}
