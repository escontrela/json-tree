package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ViewerCapabilityPresentationResolverTest {

  private final ViewerCapabilityPresentationResolver resolver =
      new ViewerCapabilityPresentationResolver();

  @Test
  void resolvesFullModePresentation() {
    ViewerCapabilityPresentation presentation = resolver.resolve(fullResult());

    assertTrue(presentation.rawJsonEnabled());
    assertTrue(presentation.searchEnabled());
    assertTrue(presentation.outlineEnabled());
    assertEquals("Copy tree", presentation.copyButtonText());
    assertEquals("Valid", presentation.validationBadgeText());
    assertEquals("status-valid", presentation.validationBadgeStyleClass());
    assertEquals("", presentation.fileMetaSuffix());
    assertEquals("Rendered 12 lines", presentation.footerStatus());
    assertEquals("VALID", presentation.statusState());
  }

  @Test
  void resolvesLargePreviewPresentation() {
    ViewerCapabilityPresentation presentation = resolver.resolve(largePreviewResult());

    assertFalse(presentation.rawJsonEnabled());
    assertFalse(presentation.searchEnabled());
    assertTrue(presentation.outlineEnabled());
    assertEquals("Copy preview", presentation.copyButtonText());
    assertEquals("Preview", presentation.validationBadgeText());
    assertEquals("status-accent", presentation.validationBadgeStyleClass());
    assertEquals(" • bounded large preview", presentation.fileMetaSuffix());
    assertEquals("PREVIEW", presentation.statusState());
    assertTrue(presentation.outlineStateMessage().contains("bounded outline minimap"));
  }

  @Test
  void restoresFullModePresentationAfterLargePreview() {
    ViewerCapabilityPresentation largePreview = resolver.resolve(largePreviewResult());
    ViewerCapabilityPresentation full = resolver.resolve(fullResult());

    assertFalse(largePreview.rawJsonEnabled());
    assertTrue(full.rawJsonEnabled());
    assertEquals("Copy tree", full.copyButtonText());
    assertEquals("VALID", full.statusState());
  }

  @Test
  void resolvesNonRenderableFullModeWithoutAsciiTree() {
    ViewerCapabilityPresentation presentation = resolver.resolve(unreadableFullResult());

    assertTrue(presentation.rawJsonEnabled());
    assertTrue(presentation.searchEnabled());
    assertTrue(presentation.outlineEnabled());
    assertEquals("Rendered 0 lines", presentation.footerStatus());
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
        new AsciiTreeDocument("root", "root\n└─ name: \"small\"", 12),
        null,
        JsonInspectionMode.FULL,
        JsonViewerCapabilities.full());
  }

  private JsonViewerLoadResult largePreviewResult() {
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
        new AsciiTreeDocument("root", "root\n... preview truncated after 400 lines", 400),
        null,
        JsonInspectionMode.LARGE_PREVIEW,
        JsonViewerCapabilities.largePreview());
  }

  private JsonViewerLoadResult unreadableFullResult() {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/missing.json"),
            "missing.json",
            0,
            false,
            false,
            false,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(
            JsonValidationStatus.PARSING_ERROR, "JSON file is not available.", null, null),
        null,
        null,
        JsonInspectionMode.FULL,
        JsonViewerCapabilities.full());
  }
}
