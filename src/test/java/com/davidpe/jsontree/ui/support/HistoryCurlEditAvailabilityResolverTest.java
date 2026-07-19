package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.DocumentFormat;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class HistoryCurlEditAvailabilityResolverTest {

  private final HistoryCurlEditAvailabilityResolver resolver =
      new HistoryCurlEditAvailabilityResolver();

  @Test
  void enablesEditOnlyForCurlBackedEntriesWithStoredProvenance() {
    ImportedJsonFile curlEntry =
        new ImportedJsonFile(
            "stored.json",
            "remote.json",
            Instant.parse("2026-07-19T14:00:00Z"),
            128L,
            8,
            true,
            false,
            DocumentFormat.JSON,
            JsonDocumentSourceKind.CURL,
            "curl https://example.com/items");

    assertTrue(resolver.supports(curlEntry));
  }

  @Test
  void rejectsNonCurlOrMissingProvenanceEntries() {
    ImportedJsonFile localEntry =
        new ImportedJsonFile(
            "stored.json",
            "local.json",
            Instant.parse("2026-07-19T14:00:00Z"),
            128L,
            8,
            true,
            false,
            DocumentFormat.JSON,
            JsonDocumentSourceKind.LOCAL_FILE,
            null);
    ImportedJsonFile brokenCurlEntry =
        new ImportedJsonFile(
            "stored.json",
            "remote.json",
            Instant.parse("2026-07-19T14:00:00Z"),
            128L,
            8,
            true,
            false,
            DocumentFormat.JSON,
            JsonDocumentSourceKind.CURL,
            "   ");

    assertFalse(resolver.supports(localEntry));
    assertFalse(resolver.supports(brokenCurlEntry));
  }
}
