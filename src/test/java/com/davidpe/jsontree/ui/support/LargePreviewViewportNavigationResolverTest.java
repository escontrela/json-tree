package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.model.LargePreviewPageRange;
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

class LargePreviewViewportNavigationResolverTest {

  private final LargePreviewViewportNavigationResolver resolver =
      new LargePreviewViewportNavigationResolver(
          new LargePreviewViewportStateResolver(),
          new LargePreviewOutlineViewportStateResolver(
              new OutlineMinimapScrollMapper(), new LargePreviewViewportStateResolver()));

  @Test
  void resolvesOutlineDrivenMovementThroughSharedViewportState() {
    LargePreviewViewportState targetState =
        resolver
            .resolveForOutlinePointer(largePreviewResult(1), 110.0, 220.0, 120.0, 1_000.0)
            .orElseThrow();

    assertEquals(2, targetState.currentPageIndex());
    assertEquals(400D / 999D, targetState.globalScrollValue());
  }

  @Test
  void resolvesViewerDrivenMovementThroughSharedViewportState() {
    LargePreviewViewportState targetState =
        resolver.resolveForScroll(largePreviewResult(1), 0.50).orElseThrow();

    assertEquals(2, targetState.currentPageIndex());
    assertEquals(3, targetState.currentPageNumber());
  }

  @Test
  void resolvesPreviousAndNextThroughSharedViewportState() {
    JsonViewerLoadResult result = largePreviewResult(2);
    LargePreviewViewportState currentState = resolver.resolveForPage(result, 2).orElseThrow();

    LargePreviewViewportState previousState =
        resolver.resolveForRelativePage(result, currentState, -1).orElseThrow();
    LargePreviewViewportState nextState =
        resolver.resolveForRelativePage(result, currentState, 1).orElseThrow();

    assertEquals(1, previousState.currentPageIndex());
    assertEquals(3, nextState.currentPageIndex());
  }

  @Test
  void supportsRepeatedAlternatingNavigationWithoutPageDrift() {
    JsonViewerLoadResult result = largePreviewResult(0);
    LargePreviewViewportState currentState = resolver.resolveForPage(result, 0).orElseThrow();

    int[] deltas = {1, 1, -1, 1, -1};
    int[] expectedPages = {1, 2, 1, 2, 1};
    for (int index = 0; index < deltas.length; index++) {
      currentState = resolver.resolveForRelativePage(result, currentState, deltas[index]).orElseThrow();
      assertEquals(expectedPages[index], currentState.currentPageIndex());
    }
  }

  @Test
  void returnsNoSharedViewportStateAfterRestoringFullMode() {
    assertTrue(resolver.resolveForScroll(fullResult(), 0.50).isEmpty());
    assertTrue(
        resolver
            .resolveForRelativePage(fullResult(), LargePreviewViewportState.inactive(), 1)
            .isEmpty());
  }

  private JsonViewerLoadResult largePreviewResult(int currentPageIndex) {
    LargePreviewPagedSession session =
        LargePreviewPagedSession.initializing(
                "session-1", LargePreviewSessionSource.local(Path.of("/tmp/large.json")))
            .withKnownTotals(
                5,
                1_000L,
                List.of(
                    new LargePreviewPageRange(0, 0L, 200),
                    new LargePreviewPageRange(1, 200L, 200),
                    new LargePreviewPageRange(2, 400L, 200),
                    new LargePreviewPageRange(3, 600L, 200),
                    new LargePreviewPageRange(4, 800L, 200)))
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
}
