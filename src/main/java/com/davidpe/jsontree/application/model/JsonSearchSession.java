package com.davidpe.jsontree.application.model;

import java.util.List;
import java.util.Optional;

public record JsonSearchSession(
    String sourceIdentity,
    String query,
    List<JsonSearchMatch> matches,
    int activeMatchIndex
) {

  public JsonSearchSession {
    matches = List.copyOf(matches);
    if (matches.isEmpty() && activeMatchIndex != -1) {
      throw new IllegalArgumentException("Empty search sessions must use -1 as active match index.");
    }
    if (!matches.isEmpty() && (activeMatchIndex < 0 || activeMatchIndex >= matches.size())) {
      throw new IllegalArgumentException("Active match index is outside the available search matches.");
    }
  }

  public int totalMatches() {
    return matches.size();
  }

  public boolean hasMatches() {
    return !matches.isEmpty();
  }

  public Optional<JsonSearchMatch> activeMatch() {
    if (!hasMatches()) {
      return Optional.empty();
    }
    return Optional.of(matches.get(activeMatchIndex));
  }

  public JsonSearchSession withActiveMatchIndex(int nextActiveMatchIndex) {
    return new JsonSearchSession(sourceIdentity, query, matches, nextActiveMatchIndex);
  }
}
