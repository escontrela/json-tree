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

class LargePreviewViewportStateResolverTest {

  private final LargePreviewViewportStateResolver resolver =
      new LargePreviewViewportStateResolver();

  @Test
  void resolvesViewportStateFromGlobalScroll() {
    LargePreviewViewportState state = resolver.resolveForScroll(largePreviewResult(), 0.50).orElseThrow();

    assertTrue(state.active());
    assertEquals(2, state.currentPageIndex());
    assertEquals(5, state.totalPages());
    assertEquals(0.50, state.globalScrollValue());
  }

  @Test
  void resolvesViewportStateFromExplicitPageAnchor() {
    LargePreviewViewportState state = resolver.resolveForPage(largePreviewResult(), 3).orElseThrow();

    assertEquals(3, state.currentPageIndex());
    assertEquals(4, state.currentPageNumber());
    assertEquals(600D / 999D, state.globalScrollValue());
    assertTrue(state.previousEnabled());
    assertTrue(state.nextEnabled());
  }

  private JsonViewerLoadResult largePreviewResult() {
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
            .withCurrentPageIndex(1);
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
        new AsciiTreeDocument("root", "root\n├─ page: 1", 2),
        null,
        JsonInspectionMode.LARGE_PREVIEW,
        JsonViewerCapabilities.largePreview(),
        session);
  }
}
