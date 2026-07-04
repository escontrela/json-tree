package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.ClipboardJsonImportResult;
import com.davidpe.jsontree.application.model.ClipboardJsonImportStatus;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.application.port.out.ClipboardPort;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClipboardJsonImportServiceTest {

  @TempDir
  java.nio.file.Path tempDir;

  @Test
  void importsValidClipboardJsonIntoViewerWorkflow() {
    ClipboardJsonImportService service =
        new ClipboardJsonImportService(
            readableClipboard("{\"name\":\"json-tree\"}"),
            viewerWorkflowService(),
            new ObjectMapper(),
            fixedClock(),
            tempDir);

    ClipboardJsonImportResult result = service.importFromClipboard();

    assertTrue(result.successful());
    assertEquals(ClipboardJsonImportStatus.VALID_JSON, result.status());
    assertNotNull(result.loadResult());
    assertEquals(expectedClipboardFileName(), result.loadResult().importResult().fileName());
    assertEquals(JsonDocumentSourceKind.CLIPBOARD, result.loadResult().importResult().sourceKind());
    assertTrue(result.loadResult().hasRenderableTree());
    assertTrue(result.loadResult().importResult().path().startsWith(tempDir));
  }

  @Test
  void reportsEmptyClipboardWhenNoTextIsAvailable() {
    ClipboardJsonImportResult result =
        new ClipboardJsonImportService(
                emptyClipboard(),
                viewerWorkflowService(),
                new ObjectMapper(),
                fixedClock(),
                tempDir)
            .importFromClipboard();

    assertFalse(result.successful());
    assertEquals(ClipboardJsonImportStatus.EMPTY_CLIPBOARD, result.status());
  }

  @Test
  void reportsUnreadableClipboardWhenAdapterFails() {
    ClipboardJsonImportResult result =
        new ClipboardJsonImportService(
                failingClipboard(),
                viewerWorkflowService(),
                new ObjectMapper(),
                fixedClock(),
                tempDir)
            .importFromClipboard();

    assertFalse(result.successful());
    assertEquals(ClipboardJsonImportStatus.UNREADABLE_CLIPBOARD, result.status());
  }

  @Test
  void rejectsInvalidJsonClipboardTextWithoutCreatingViewerDocument() {
    ClipboardJsonImportResult result =
        new ClipboardJsonImportService(
                readableClipboard("{invalid"),
                viewerWorkflowService(),
                new ObjectMapper(),
                fixedClock(),
                tempDir)
            .importFromClipboard();

    assertFalse(result.successful());
    assertEquals(ClipboardJsonImportStatus.INVALID_JSON, result.status());
    assertTrue(result.message().contains("Clipboard text is not valid JSON"));
  }

  @Test
  void repeatedImportsCreateDistinctClipboardMaterializedFiles() {
    JsonViewerWorkflowService workflowService = viewerWorkflowService();
    ClipboardJsonImportService service =
        new ClipboardJsonImportService(
            readableClipboard("{\"name\":\"json-tree\"}"),
            workflowService,
            new ObjectMapper(),
            fixedClock(),
            tempDir);

    ClipboardJsonImportResult first = service.importFromClipboard();
    ClipboardJsonImportResult second = service.importFromClipboard();

    assertTrue(first.successful());
    assertTrue(second.successful());
    assertEquals(expectedClipboardFileName(), first.loadResult().importResult().fileName());
    assertEquals(expectedClipboardCollisionFileName(), second.loadResult().importResult().fileName());
  }

  @Test
  void invalidClipboardDoesNotReplacePreviouslyImportedDocument() {
    JsonViewerWorkflowService workflowService = viewerWorkflowService();
    ClipboardJsonImportService validService =
        new ClipboardJsonImportService(
            readableClipboard("{\"name\":\"json-tree\"}"),
            workflowService,
            new ObjectMapper(),
            fixedClock(),
            tempDir);
    ClipboardJsonImportService invalidService =
        new ClipboardJsonImportService(
            readableClipboard("{invalid"),
            workflowService,
            new ObjectMapper(),
            fixedClock(),
            tempDir);

    ClipboardJsonImportResult validResult = validService.importFromClipboard();
    ClipboardJsonImportResult invalidResult = invalidService.importFromClipboard();

    assertTrue(validResult.successful());
    assertFalse(invalidResult.successful());
    assertEquals(
        expectedClipboardFileName(),
        workflowService.currentView().orElseThrow().importResult().fileName());
  }

  private JsonViewerWorkflowService viewerWorkflowService() {
    return new JsonViewerWorkflowService(
        validValidationPort(),
        new InMemoryHistoryRepository(),
        renderSimpleAsciiTree(),
        inspectionModeResolver(),
        fixedClock());
  }

  private JsonInspectionModeResolver inspectionModeResolver() {
    return new JsonInspectionModeResolver(new LargePreviewProperties());
  }

  private JsonValidationPort validValidationPort() {
    return path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
  }

  private AsciiTreeRendererPort renderSimpleAsciiTree() {
    return path -> new AsciiTreeDocument("root", "root\n└─ name: \"json-tree\"", 2);
  }

  private Clock fixedClock() {
    return Clock.fixed(Instant.parse("2026-07-02T10:15:30Z"), ZoneOffset.UTC);
  }

  private String expectedClipboardFileName() {
    return "clipboard-"
        + DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault())
            .format(fixedClock().instant())
        + ".json";
  }

  private String expectedClipboardCollisionFileName() {
    return expectedClipboardFileName().replace(".json", "-2.json");
  }

  private ClipboardPort readableClipboard(String text) {
    return new ClipboardPort() {
      @Override
      public void copy(String text) {
      }

      @Override
      public Optional<String> readText() {
        return Optional.of(text);
      }
    };
  }

  private ClipboardPort emptyClipboard() {
    return new ClipboardPort() {
      @Override
      public void copy(String text) {
      }

      @Override
      public Optional<String> readText() {
        return Optional.empty();
      }
    };
  }

  private ClipboardPort failingClipboard() {
    return new ClipboardPort() {
      @Override
      public void copy(String text) {
      }

      @Override
      public Optional<String> readText() {
        throw new IllegalStateException("Clipboard unavailable");
      }
    };
  }

  private static final class InMemoryHistoryRepository implements JsonHistoryRepository {

    @Override
    public List<ImportedJsonFile> findAll() {
      return List.of();
    }

    @Override
    public Optional<ImportedJsonFile> findByStoredName(String storedName) {
      return Optional.empty();
    }

    @Override
    public Optional<Path> resolveStoredJsonPath(String storedName) {
      return Optional.empty();
    }

    @Override
    public Optional<String> readStoredJson(String storedName) {
      return Optional.empty();
    }

    @Override
    public void save(ImportedJsonFile importedJsonFile, String jsonContent) {
    }

    @Override
    public Optional<ImportedJsonFile> updateFavorite(String storedName, boolean favorite) {
      return Optional.empty();
    }

    @Override
    public void deleteByStoredName(String storedName) {
    }
  }
}
