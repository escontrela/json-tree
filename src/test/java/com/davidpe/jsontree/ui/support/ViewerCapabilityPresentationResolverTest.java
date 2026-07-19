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
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ViewerCapabilityPresentationResolverTest {

  private final ViewerCapabilityPresentationResolver resolver =
      new ViewerCapabilityPresentationResolver();

  @Test
  void resolvesFullModePresentation() {
    ViewerCapabilityPresentation presentation =
        resolver.resolve(fullResult(), ViewerPresentationMode.ASCII_TREE);

    assertTrue(presentation.rawJsonEnabled());
    assertTrue(presentation.structureEnabled());
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
    ViewerCapabilityPresentation presentation =
        resolver.resolve(largePreviewResult(), ViewerPresentationMode.RAW_JSON);

    assertFalse(presentation.rawJsonEnabled());
    assertFalse(presentation.structureEnabled());
    assertFalse(presentation.searchEnabled());
    assertFalse(presentation.outlineEnabled());
    assertEquals("Copy preview", presentation.copyButtonText());
    assertEquals("Preview", presentation.validationBadgeText());
    assertEquals("status-accent", presentation.validationBadgeStyleClass());
    assertEquals(" • byte-paged large preview", presentation.fileMetaSuffix());
    assertEquals("PREVIEW", presentation.statusState());
    assertTrue(presentation.outlineStateMessage().contains("disables the outline"));
    assertTrue(presentation.footerStatus().contains("current large-file page"));
  }

  @Test
  void restoresFullModePresentationAfterLargePreview() {
    ViewerCapabilityPresentation largePreview =
        resolver.resolve(largePreviewResult(), ViewerPresentationMode.RAW_JSON);
    ViewerCapabilityPresentation full =
        resolver.resolve(fullResult(), ViewerPresentationMode.ASCII_TREE);

    assertFalse(largePreview.rawJsonEnabled());
    assertTrue(full.rawJsonEnabled());
    assertEquals("Copy tree", full.copyButtonText());
    assertEquals("VALID", full.statusState());
  }

  @Test
  void resolvesNonRenderableFullModeWithoutAsciiTree() {
    ViewerCapabilityPresentation presentation =
        resolver.resolve(unreadableFullResult(), ViewerPresentationMode.ASCII_TREE);

    assertTrue(presentation.rawJsonEnabled());
    assertFalse(presentation.structureEnabled());
    assertTrue(presentation.searchEnabled());
    assertTrue(presentation.outlineEnabled());
    assertEquals("Rendered 0 lines", presentation.footerStatus());
  }

  @Test
  void disablesSearchAndOutlineWhileKeepingStructureEntryPointAvailable() {
    ViewerCapabilityPresentation presentation =
        resolver.resolve(fullResult(), ViewerPresentationMode.STRUCTURE);

    assertTrue(presentation.rawJsonEnabled());
    assertTrue(presentation.structureEnabled());
    assertFalse(presentation.searchEnabled());
    assertFalse(presentation.outlineEnabled());
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
        JsonViewerCapabilities.full(),
        null);
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
        JsonViewerCapabilities.largePreview(),
        null);
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
        JsonViewerCapabilities.full(),
        null);
  }
}
