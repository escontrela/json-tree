package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewPageRange;
import com.davidpe.jsontree.application.model.LargePreviewPageState;
import com.davidpe.jsontree.application.model.LargePreviewPagedSession;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class LargePreviewPageNavigationStateResolverTest {

  private final LargePreviewPageNavigationStateResolver resolver =
      new LargePreviewPageNavigationStateResolver();

  @Test
  void hidesControlsForFullMode() {
    LargePreviewPageNavigationState state = resolver.resolve(fullResult());

    assertFalse(state.visible());
  }

  @Test
  void resolvesFirstPageAndLastPageButtonStates() {
    LargePreviewPageNavigationState firstPageState = resolver.resolve(largePreviewResult(0, 4));
    LargePreviewPageNavigationState lastPageState = resolver.resolve(largePreviewResult(3, 4));

    assertTrue(firstPageState.visible());
    assertEquals(1, firstPageState.currentPageNumber());
    assertEquals(4, firstPageState.totalPages());
    assertFalse(firstPageState.previousEnabled());
    assertTrue(firstPageState.nextEnabled());

    assertEquals(4, lastPageState.currentPageNumber());
    assertTrue(lastPageState.previousEnabled());
    assertFalse(lastPageState.nextEnabled());
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
    List<LargePreviewPageRange> pageRanges =
        java.util.stream.IntStream.range(0, totalPages)
            .mapToObj(pageIndex -> new LargePreviewPageRange(pageIndex, pageIndex * 200L, 200))
            .toList();
    LargePreviewPagedSession session =
        LargePreviewPagedSession.initializing(
                "session-1", LargePreviewSessionSource.local(Path.of("/tmp/large.json")))
            .withKnownTotals(totalPages, totalPages * 200L, pageRanges)
            .withCurrentPageIndex(currentPageIndex)
            .withPageStates(
                java.util.stream.IntStream.range(0, totalPages)
                    .mapToObj(
                        pageIndex ->
                            LargePreviewPageState.available(
                                pageIndex, pageIndex == currentPageIndex, true, 200))
                    .toList());
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
