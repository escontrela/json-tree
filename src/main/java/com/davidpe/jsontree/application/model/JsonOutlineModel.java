package com.davidpe.jsontree.application.model;

import java.util.List;

public record JsonOutlineModel(
    List<JsonOutlineEntry> entries,
    int maxDepth
) {

  public JsonOutlineModel {
    entries = List.copyOf(entries);
    if (maxDepth < 0) {
      throw new IllegalArgumentException("Outline max depth must be zero or greater.");
    }
  }

  public static JsonOutlineModel empty() {
    return new JsonOutlineModel(List.of(), 0);
  }

  public int totalEntries() {
    return entries.size();
  }

  public boolean emptyModel() {
    return entries.isEmpty();
  }
}
