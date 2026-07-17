package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class LargePreviewDocumentScrollResolverTest {

  private final LargePreviewDocumentScrollResolver resolver =
      new LargePreviewDocumentScrollResolver();

  @Test
  void resolvesDocumentWideScrollIntoPageIndexes() {
    JsonViewerLoadResult result = largePreviewResult(0);

    assertEquals(0, resolver.targetPage(result, 0.0).orElseThrow());
    assertEquals(2, resolver.targetPage(result, 0.50).orElseThrow());
    assertEquals(4, resolver.targetPage(result, 1.0).orElseThrow());
  }

  @Test
  void exposesStablePageStartAnchors() {
    JsonViewerLoadResult result = largePreviewResult(2);

    assertEquals(400D / 999D, resolver.pageStartScrollValue(result, 2).orElseThrow());
    assertTrue(resolver.pageStartScrollValue(fullResult(), 0).isEmpty());
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

  private JsonViewerLoadResult largePreviewResult(int currentPageIndex) {
    LargePreviewPagedSession session =
        LargePreviewPagedSession.initializing(
                "session-1", LargePreviewSessionSource.local(Path.of("/tmp/large.json")))
            .withPageState(LargePreviewPageState.available(0, false, true, 200))
            .withPageState(LargePreviewPageState.available(1, false, true, 200))
            .withPageState(LargePreviewPageState.available(2, true, true, 200))
            .withPageState(LargePreviewPageState.available(3, false, true, 200))
            .withPageState(LargePreviewPageState.available(4, false, true, 200))
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
}
