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

class LargePreviewOutlineStepResolverTest {

  private final LargePreviewOutlineStepResolver resolver = new LargePreviewOutlineStepResolver();

  @Test
  void buildsPageAnchorsAndMarksCurrentPageActive() {
    LargePreviewPagedSession session =
        LargePreviewPagedSession.initializing(
                "session-1", LargePreviewSessionSource.local(Path.of("/tmp/large.json")))
            .withKnownTotals(
                3,
                900L,
                List.of(
                    new LargePreviewPageRange(0, 0L, 300),
                    new LargePreviewPageRange(1, 300L, 300),
                    new LargePreviewPageRange(2, 600L, 300)))
            .withCurrentPageIndex(1);
    JsonViewerLoadResult result =
        new JsonViewerLoadResult(
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

    List<LargePreviewOutlineStep> steps = resolver.resolve(result);

    assertEquals(3, steps.size());
    assertEquals("Page 1", steps.get(0).title());
    assertEquals("Lines 1-300", steps.get(0).meta());
    assertTrue(steps.get(1).active());
    assertEquals(300D / 899D, steps.get(1).documentScrollValue());
  }
}
