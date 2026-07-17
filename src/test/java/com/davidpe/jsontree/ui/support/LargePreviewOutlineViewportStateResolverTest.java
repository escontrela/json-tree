package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class LargePreviewOutlineViewportStateResolverTest {

  private final LargePreviewOutlineViewportStateResolver resolver =
      new LargePreviewOutlineViewportStateResolver(
          new OutlineMinimapScrollMapper(), new LargePreviewViewportStateResolver());

  @Test
  void snapsOutlinePointerToDeterministicLargePageAnchor() {
    LargePreviewViewportState targetState =
        resolver
            .resolveForPointer(largePreviewResult(), 110.0, 220.0, 120.0, 1_000.0)
            .orElseThrow();

    assertEquals(2, targetState.currentPageIndex());
    assertEquals(400D / 999D, targetState.globalScrollValue());
  }

  @Test
  void supportsBackwardAndForwardOutlineJumps() {
    LargePreviewViewportState upperState =
        resolver
            .resolveForPointer(largePreviewResult(), 24.0, 220.0, 120.0, 1_000.0)
            .orElseThrow();
    LargePreviewViewportState lowerState =
        resolver
            .resolveForPointer(largePreviewResult(), 196.0, 220.0, 120.0, 1_000.0)
            .orElseThrow();

    assertEquals(0, upperState.currentPageIndex());
    assertEquals(4, lowerState.currentPageIndex());
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
