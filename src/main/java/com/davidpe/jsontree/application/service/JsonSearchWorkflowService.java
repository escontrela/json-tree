package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonSearchExecutionResult;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonSearchWorkflowService {

  private final JsonViewerWorkflowService viewerWorkflowService;
  private final RegexTextSearchService regexTextSearchService;

  private JsonSearchSession currentSession;

  public JsonSearchWorkflowService(JsonViewerWorkflowService viewerWorkflowService) {
    this(viewerWorkflowService, new RegexTextSearchService());
  }

  @Autowired
  public JsonSearchWorkflowService(
      JsonViewerWorkflowService viewerWorkflowService,
      RegexTextSearchService regexTextSearchService) {
    this.viewerWorkflowService = viewerWorkflowService;
    this.regexTextSearchService = regexTextSearchService;
  }

  public JsonSearchExecutionResult activateSearch(String sourceIdentity, String rawQuery) {
    String sourceText = viewerWorkflowService.currentViewRawJson().orElse(null);
    if (sourceText == null || sourceText.isBlank()) {
      return JsonSearchExecutionResult.failure("No source text is available for search.");
    }
    JsonSearchExecutionResult result =
        regexTextSearchService.search(sourceIdentity, rawQuery, sourceText);
    if (result.successful()) {
      currentSession = result.session();
    }
    return result;
  }

  public Optional<JsonSearchSession> currentSession() {
    return Optional.ofNullable(currentSession);
  }

  public Optional<JsonSearchSession> moveToPreviousMatch() {
    if (currentSession == null || !currentSession.hasMatches()) {
      return Optional.ofNullable(currentSession);
    }
    if (currentSession.totalMatches() == 1) {
      return Optional.of(currentSession);
    }

    int previousIndex =
        (currentSession.activeMatchIndex() - 1 + currentSession.totalMatches())
            % currentSession.totalMatches();
    currentSession = currentSession.withActiveMatchIndex(previousIndex);
    return Optional.of(currentSession);
  }

  public Optional<JsonSearchSession> moveToNextMatch() {
    if (currentSession == null || !currentSession.hasMatches()) {
      return Optional.ofNullable(currentSession);
    }
    if (currentSession.totalMatches() == 1) {
      return Optional.of(currentSession);
    }

    int nextIndex = (currentSession.activeMatchIndex() + 1) % currentSession.totalMatches();
    currentSession = currentSession.withActiveMatchIndex(nextIndex);
    return Optional.of(currentSession);
  }

  public void clear() {
    currentSession = null;
  }

  public void clearIfSourceChanged(String sourceIdentity) {
    if (currentSession == null) {
      return;
    }
    if (!currentSession.sourceIdentity().equals(sourceIdentity)) {
      currentSession = null;
    }
  }
}
