package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonSearchExecutionResult;
import com.davidpe.jsontree.application.model.JsonSearchMatch;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;

@Service
public class JsonSearchWorkflowService {

  private final JsonViewerWorkflowService viewerWorkflowService;

  private JsonSearchSession currentSession;

  public JsonSearchWorkflowService(JsonViewerWorkflowService viewerWorkflowService) {
    this.viewerWorkflowService = viewerWorkflowService;
  }

  public JsonSearchExecutionResult activateSearch(String sourceIdentity, String rawQuery) {
    String query = rawQuery == null ? "" : rawQuery.trim();
    if (query.isBlank()) {
      return JsonSearchExecutionResult.failure("Enter a regular expression.");
    }

    String rawJson =
        viewerWorkflowService
            .currentViewRawJson()
            .orElse(null);
    if (rawJson == null || rawJson.isBlank()) {
      return JsonSearchExecutionResult.failure("No JSON source is available for search.");
    }

    Pattern pattern;
    try {
      pattern = Pattern.compile(query);
    } catch (PatternSyntaxException exception) {
      return JsonSearchExecutionResult.failure(
          exception.getDescription() == null || exception.getDescription().isBlank()
              ? "Invalid regular expression."
              : exception.getDescription());
    }

    List<JsonSearchMatch> matches = collectMatches(pattern, rawJson);
    currentSession =
        new JsonSearchSession(
            sourceIdentity,
            query,
            matches,
            matches.isEmpty() ? -1 : 0
        );
    return JsonSearchExecutionResult.success(currentSession);
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

  private List<JsonSearchMatch> collectMatches(Pattern pattern, String rawJson) {
    List<JsonSearchMatch> matches = new ArrayList<>();
    Matcher matcher = pattern.matcher(rawJson);
    while (matcher.find()) {
      if (matcher.start() == matcher.end()) {
        continue;
      }
      matches.add(new JsonSearchMatch(matcher.start(), matcher.end(), matcher.group()));
    }
    return matches;
  }
}
