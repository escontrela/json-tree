package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonSearchExecutionResult;
import com.davidpe.jsontree.application.model.JsonSearchMatch;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;

/**
 * Executes regex search over arbitrary read-only text while preserving the shared search session
 * contract already used by the viewer workflows.
 */
@Service
public class RegexTextSearchService {

  public JsonSearchExecutionResult search(String sourceIdentity, String rawQuery, String content) {
    String query = rawQuery == null ? "" : rawQuery.trim();
    if (query.isBlank()) {
      return JsonSearchExecutionResult.failure("Enter a regular expression.");
    }
    if (content == null || content.isBlank()) {
      return JsonSearchExecutionResult.failure("No text is available for search.");
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

    List<JsonSearchMatch> matches = collectMatches(pattern, content);
    return JsonSearchExecutionResult.success(
        new JsonSearchSession(
            sourceIdentity, query, matches, matches.isEmpty() ? -1 : 0));
  }

  private List<JsonSearchMatch> collectMatches(Pattern pattern, String content) {
    List<JsonSearchMatch> matches = new ArrayList<>();
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      if (matcher.start() == matcher.end()) {
        continue;
      }
      matches.add(new JsonSearchMatch(matcher.start(), matcher.end(), matcher.group()));
    }
    return matches;
  }
}
