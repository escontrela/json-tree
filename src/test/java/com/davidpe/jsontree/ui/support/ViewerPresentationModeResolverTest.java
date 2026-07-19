package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.DocumentFormat;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ViewerPresentationModeResolverTest {

  private final ViewerPresentationModeResolver resolver = new ViewerPresentationModeResolver();

  @Test
  void keepsJsonModesDeterministic() {
    JsonViewerLoadResult jsonResult = jsonResult(JsonInspectionMode.FULL);

    assertEquals(
        ViewerPresentationMode.ASCII_TREE,
        resolver.resolve(ViewerPresentationMode.ASCII_TREE, jsonResult));
    assertEquals(
        ViewerPresentationMode.RAW_JSON,
        resolver.resolve(ViewerPresentationMode.RAW_JSON, jsonResult));
    assertEquals(
        ViewerPresentationMode.STRUCTURE,
        resolver.resolve(ViewerPresentationMode.STRUCTURE, jsonResult));
  }

  @Test
  void defaultsMarkdownToRenderedAndAllowsExplicitRawMode() {
    JsonViewerLoadResult markdownResult = markdownResult(JsonInspectionMode.FULL);

    assertEquals(
        ViewerPresentationMode.MARKDOWN_RENDERED,
        resolver.resolve(ViewerPresentationMode.ASCII_TREE, markdownResult));
    assertEquals(
        ViewerPresentationMode.MARKDOWN_RENDERED,
        resolver.resolve(ViewerPresentationMode.MARKDOWN_RENDERED, markdownResult));
    assertEquals(
        ViewerPresentationMode.RAW_MARKDOWN,
        resolver.resolve(ViewerPresentationMode.RAW_MARKDOWN, markdownResult));
  }

  @Test
  void keepsLargePreviewMarkdownInRawOnlyMode() {
    JsonViewerLoadResult markdownLargePreview = markdownResult(JsonInspectionMode.LARGE_PREVIEW);

    assertEquals(
        ViewerPresentationMode.RAW_MARKDOWN,
        resolver.resolve(ViewerPresentationMode.MARKDOWN_RENDERED, markdownLargePreview));
    assertEquals(
        ViewerPresentationMode.RAW_MARKDOWN,
        resolver.resolve(ViewerPresentationMode.RAW_MARKDOWN, markdownLargePreview));
  }

  private JsonViewerLoadResult jsonResult(JsonInspectionMode inspectionMode) {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/sample.json"),
            "sample.json",
            128L,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON", null, null),
        new AsciiTreeDocument("root", "root\n└─ id: 1", 2),
        null,
        inspectionMode,
        inspectionMode == JsonInspectionMode.LARGE_PREVIEW
            ? JsonViewerCapabilities.largePreview()
            : JsonViewerCapabilities.full(),
        null);
  }

  private JsonViewerLoadResult markdownResult(JsonInspectionMode inspectionMode) {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/notes.md"),
            "notes.md",
            128L,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE,
            DocumentFormat.MARKDOWN),
        new JsonValidationResult(JsonValidationStatus.VALID, "Markdown ready", null, null),
        new AsciiTreeDocument("notes.md", "# Heading\n\ncontent", 3),
        null,
        inspectionMode,
        inspectionMode == JsonInspectionMode.LARGE_PREVIEW
            ? JsonViewerCapabilities.largePreview()
            : new JsonViewerCapabilities(true, true, true),
        null);
  }
}
