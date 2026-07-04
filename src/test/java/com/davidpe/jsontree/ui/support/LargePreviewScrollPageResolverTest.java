package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewPageState;
import com.davidpe.jsontree.application.model.LargePreviewPagedSession;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LargePreviewScrollPageResolverTest {

  private final LargePreviewScrollPageResolver resolver = new LargePreviewScrollPageResolver();

  @Test
  void resolvesForwardPageSwapNearBottomForLargePreview() {
    JsonViewerLoadResult result = largePreviewResult(2, 5);

    assertTrue(resolver.targetPage(result, 0.99).isPresent());
    assertEquals(3, resolver.targetPage(result, 0.99).getAsInt());
  }

  @Test
  void resolvesBackwardPageSwapNearTopForLargePreview() {
    JsonViewerLoadResult result = largePreviewResult(3, 5);

    assertTrue(resolver.targetPage(result, 0.01).isPresent());
    assertEquals(2, resolver.targetPage(result, 0.01).getAsInt());
  }

  @Test
  void ignoresFullModeAndLargePreviewEndOfDocument() {
    assertTrue(resolver.targetPage(fullResult(), 0.99).isEmpty());
    assertTrue(resolver.targetPage(largePreviewResult(4, 5), 0.99).isEmpty());
    assertFalse(resolver.targetPage(largePreviewResult(0, 5), 0.50).isPresent());
  }

  private JsonViewerLoadResult fullResult() {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/small.json"),
            "small.json",
            128,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
        new AsciiTreeDocument("root", "root\n└─ id: 1", 2),
        null,
        JsonInspectionMode.FULL,
        JsonViewerCapabilities.full(),
        null);
  }

  private JsonViewerLoadResult largePreviewResult(int currentPageIndex, int totalPages) {
    LargePreviewPagedSession session =
        LargePreviewPagedSession.initializing(
                "session-1", LargePreviewSessionSource.local(Path.of("/tmp/large.json")))
            .withPageState(LargePreviewPageState.available(0, false, true, 400))
            .withPageState(LargePreviewPageState.available(1, false, true, 400))
            .withPageState(LargePreviewPageState.available(2, true, true, 400))
            .withPageState(LargePreviewPageState.available(3, false, true, 400))
            .withPageState(LargePreviewPageState.available(4, false, true, 400))
            .withKnownTotals(totalPages, 2_000L)
            .withCurrentPageIndex(currentPageIndex);
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/large.json"),
            "large.json",
            4_194_304,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
        new AsciiTreeDocument("root", "root\n├─ page: " + currentPageIndex, 2),
        null,
        JsonInspectionMode.LARGE_PREVIEW,
        JsonViewerCapabilities.largePreview(),
        session);
  }
}
