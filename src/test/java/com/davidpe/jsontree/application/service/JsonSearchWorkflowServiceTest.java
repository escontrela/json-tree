package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonSearchExecutionResult;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonSearchWorkflowServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void rejectsBlankQueriesBeforeActivatingSearch() {
    JsonSearchWorkflowService searchService = new JsonSearchWorkflowService(viewerWorkflowService());

    JsonSearchExecutionResult result = searchService.activateSearch("file:/sample.json", "   ");

    assertFalse(result.successful());
    assertEquals("Enter a regular expression.", result.errorMessage());
    assertTrue(searchService.currentSession().isEmpty());
  }

  @Test
  void rejectsInvalidRegularExpressions() {
    JsonSearchWorkflowService searchService = new JsonSearchWorkflowService(viewerWorkflowService());

    JsonSearchExecutionResult result = searchService.activateSearch("file:/sample.json", "[abc");

    assertFalse(result.successful());
    assertTrue(result.errorMessage().toLowerCase().contains("closed"));
    assertTrue(searchService.currentSession().isEmpty());
  }

  @Test
  void executesSearchAgainstCurrentRawJsonAndReturnsOrderedMatches() {
    JsonSearchWorkflowService searchService = new JsonSearchWorkflowService(viewerWorkflowService());

    JsonSearchExecutionResult result = searchService.activateSearch("file:/sample.json", "\"admin\"");

    assertTrue(result.successful());
    JsonSearchSession session = result.session();
    assertEquals("\"admin\"", session.query());
    assertEquals(1, session.totalMatches());
    assertEquals(0, session.activeMatchIndex());
    assertEquals("\"admin\"", session.activeMatch().orElseThrow().fragment());
  }

  @Test
  void validRegexWithNoMatchesStillCreatesNavigableSessionMetadata() {
    JsonSearchWorkflowService searchService = new JsonSearchWorkflowService(viewerWorkflowService());

    JsonSearchExecutionResult result = searchService.activateSearch("file:/sample.json", "missing-key");

    assertTrue(result.successful());
    assertEquals(0, result.session().totalMatches());
    assertEquals(-1, result.session().activeMatchIndex());
  }

  @Test
  void clearsExistingSessionWhenSourceIdentityChanges() {
    JsonSearchWorkflowService searchService = new JsonSearchWorkflowService(viewerWorkflowService());
    searchService.activateSearch("file:/sample.json", "David");

    searchService.clearIfSourceChanged("file:/other.json");

    assertTrue(searchService.currentSession().isEmpty());
  }

  @Test
  void movesNextAndPreviousThroughMatchesInStableWrappedOrder() {
    JsonSearchWorkflowService searchService = new JsonSearchWorkflowService(viewerWorkflowService());
    searchService.activateSearch("file:/sample.json", "\"(?:David|admin)\"");

    JsonSearchSession initial = searchService.currentSession().orElseThrow();
    assertEquals(0, initial.activeMatchIndex());
    assertEquals(2, initial.totalMatches());

    JsonSearchSession next = searchService.moveToNextMatch().orElseThrow();
    assertEquals(1, next.activeMatchIndex());
    assertEquals("\"admin\"", next.activeMatch().orElseThrow().fragment());

    JsonSearchSession wrapped = searchService.moveToNextMatch().orElseThrow();
    assertEquals(0, wrapped.activeMatchIndex());
    assertEquals("\"David\"", wrapped.activeMatch().orElseThrow().fragment());

    JsonSearchSession previous = searchService.moveToPreviousMatch().orElseThrow();
    assertEquals(1, previous.activeMatchIndex());
  }

  private JsonViewerWorkflowService viewerWorkflowService() {
    JsonValidationPort validationPort =
        path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
    AsciiTreeRendererPort rendererPort =
        path -> new AsciiTreeDocument("root", "root\n└─ roles [1]\n   └─ 0: \"admin\"", 3);

    JsonViewerWorkflowService workflowService =
        new JsonViewerWorkflowService(
            validationPort,
            new InMemoryHistoryRepository(),
            rendererPort,
            new JsonInspectionModeResolver(new LargePreviewProperties()));
    try {
      Path jsonFile =
          Files.writeString(
              tempDir.resolve("sample.json"),
              "{\"name\":\"David\",\"roles\":[\"admin\"],\"id\":42}");
      workflowService.loadFile(jsonFile);
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
    return workflowService;
  }

  private static final class InMemoryHistoryRepository implements JsonHistoryRepository {

    private final Map<String, ImportedJsonFile> entries = new ConcurrentHashMap<>();
    private final Map<String, String> storedJsonByName = new ConcurrentHashMap<>();

    @Override
    public List<ImportedJsonFile> findAll() {
      return List.copyOf(entries.values());
    }

    @Override
    public Optional<ImportedJsonFile> findByStoredName(String storedName) {
      return Optional.ofNullable(entries.get(storedName));
    }

    @Override
    public Optional<String> readStoredJson(String storedName) {
      return Optional.ofNullable(storedJsonByName.get(storedName));
    }

    @Override
    public void save(ImportedJsonFile importedJsonFile, String jsonContent) {
      entries.put(importedJsonFile.storedName(), importedJsonFile);
      storedJsonByName.put(importedJsonFile.storedName(), jsonContent);
    }

    @Override
    public Optional<ImportedJsonFile> updateFavorite(String storedName, boolean favorite) {
      ImportedJsonFile existing = entries.get(storedName);
      if (existing == null) {
        return Optional.empty();
      }
      ImportedJsonFile updated = existing.withFavorite(favorite);
      entries.put(storedName, updated);
      return Optional.of(updated);
    }

    @Override
    public void deleteByStoredName(String storedName) {
      entries.remove(storedName);
      storedJsonByName.remove(storedName);
    }
  }
}
