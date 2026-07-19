package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoomViewerSnapshotFactoryTest {

  private final ZoomViewerSnapshotFactory factory = new ZoomViewerSnapshotFactory();

  @Test
  void buildsAsciiAndRawSnapshotsForFullDocuments() {
    JsonViewerLoadResult result = renderableResult("sample.json", JsonInspectionMode.FULL);
    ViewerTextRenderPlan renderPlan =
        ViewerTextRenderPlan.normal(
            List.of(
                new ViewerTextRenderFragment(
                    "root\n└─ id: 1", "tree-default", "#2d333a", false, false)));

    assertEquals(
        "ASCII tree",
        factory
            .renderable(
                result,
                "ASCII tree",
                renderPlan,
                "tree-content",
                "1.0 KB • local import",
                ViewerPresentationMode.ASCII_TREE,
                JsonBreadcrumbModel.unavailable())
            .modeLabel());
    assertEquals(
        "Raw JSON",
        factory
            .renderable(
                result,
                "Raw JSON",
                renderPlan,
                "raw-json-content",
                "1.0 KB • local import",
                ViewerPresentationMode.RAW_JSON,
                JsonBreadcrumbModel.unavailable())
            .modeLabel());
  }

  @Test
  void buildsLargePreviewSnapshotsAgainstTheCurrentChunkPresentation() {
    JsonViewerLoadResult result = renderableResult("large.json", JsonInspectionMode.LARGE_PREVIEW);
    ViewerTextRenderPlan renderPlan =
        ViewerTextRenderPlan.normal(
            List.of(
                new ViewerTextRenderFragment(
                    "{\"chunk\":true}", "raw-json-text", "#2d333a", false, false)));

    var snapshot =
        factory.renderable(
            result,
            "Raw page",
            renderPlan,
            "raw-json-content",
            "12.3 MB • reopened from history • byte-paged large preview",
            ViewerPresentationMode.RAW_JSON,
            JsonBreadcrumbModel.unavailable());

    assertEquals("Raw page", snapshot.modeLabel());
    assertTrue(snapshot.largePreview());
    assertTrue(snapshot.windowTitle().contains("large.json"));
    assertTrue(snapshot.documentMeta().contains("byte-paged large preview"));
  }

  @Test
  void buildsStructureSnapshotsForZoomUsingTheSameSharedSnapshotContract() {
    JsonViewerLoadResult result = renderableResult("schema.json", JsonInspectionMode.FULL);
    ViewerTextRenderPlan renderPlan =
        ViewerTextRenderPlan.normal(
            List.of(
                new ViewerTextRenderFragment(
                    "root\n└─ user\n   └─ name", "tree-structure", "#2d333a", false, false)));

    var snapshot =
        factory.renderable(
            result,
            "Structure",
            renderPlan,
            "tree-content",
            "1.0 KB • local import",
            ViewerPresentationMode.STRUCTURE,
            JsonBreadcrumbModel.unavailable());

    assertEquals("Structure", snapshot.modeLabel());
    assertEquals("tree-content", snapshot.contentStyleClass());
    assertEquals(ViewerPresentationMode.STRUCTURE, snapshot.presentationMode());
    assertTrue(snapshot.windowTitle().contains("schema.json"));
  }

  @Test
  void buildsRenderedAndRawMarkdownSnapshotsUsingTheSharedContract() {
    JsonViewerLoadResult result = markdownResult("notes.md", JsonInspectionMode.FULL);
    ViewerTextRenderPlan renderPlan =
        ViewerTextRenderPlan.normal(
            List.of(
                new ViewerTextRenderFragment(
                    "Heading\n\nParagraph", "markdown-rendered-heading-1", "#2d333a", false, false)));

    var renderedSnapshot =
        factory.renderable(
            result,
            "Markdown",
            renderPlan,
            "markdown-content",
            "2.0 KB • local import",
            ViewerPresentationMode.MARKDOWN_RENDERED,
            JsonBreadcrumbModel.unavailable());
    var rawSnapshot =
        factory.renderable(
            result,
            "Raw Markdown",
            renderPlan,
            "markdown-content",
            "2.0 KB • local import",
            ViewerPresentationMode.RAW_MARKDOWN,
            JsonBreadcrumbModel.unavailable());

    assertEquals("Markdown", renderedSnapshot.modeLabel());
    assertEquals(ViewerPresentationMode.MARKDOWN_RENDERED, renderedSnapshot.presentationMode());
    assertEquals("Raw Markdown", rawSnapshot.modeLabel());
    assertEquals(ViewerPresentationMode.RAW_MARKDOWN, rawSnapshot.presentationMode());
  }

  private JsonViewerLoadResult renderableResult(String fileName, JsonInspectionMode mode) {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/" + fileName),
            fileName,
            1024L,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON", null, null),
        new AsciiTreeDocument("root", "root\n└─ id: 1", 2),
        null,
        mode,
        mode == JsonInspectionMode.LARGE_PREVIEW
            ? JsonViewerCapabilities.largePreview()
            : JsonViewerCapabilities.full(),
        null);
  }

  private JsonViewerLoadResult markdownResult(String fileName, JsonInspectionMode mode) {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/" + fileName),
            fileName,
            1024L,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE,
            com.davidpe.jsontree.domain.model.DocumentFormat.MARKDOWN),
        new JsonValidationResult(JsonValidationStatus.VALID, "Markdown ready", null, null),
        new AsciiTreeDocument(fileName, "# Heading\n\nParagraph", 3),
        null,
        mode,
        mode == JsonInspectionMode.LARGE_PREVIEW
            ? JsonViewerCapabilities.largePreview()
            : new JsonViewerCapabilities(true, true, true),
        null);
  }
}
