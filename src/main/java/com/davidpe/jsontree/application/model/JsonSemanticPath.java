package com.davidpe.jsontree.application.model;

import java.util.List;

/**
 * Semantic path to a JSON node using property names and array indexes.
 */
public record JsonSemanticPath(List<JsonSemanticPathSegment> segments) {

  public JsonSemanticPath {
    segments = List.copyOf(segments);
  }

  public static JsonSemanticPath root() {
    return new JsonSemanticPath(List.of());
  }

  public JsonSemanticPath append(JsonSemanticPathSegment segment) {
    java.util.ArrayList<JsonSemanticPathSegment> nextSegments = new java.util.ArrayList<>(segments);
    nextSegments.add(segment);
    return new JsonSemanticPath(nextSegments);
  }

  public boolean empty() {
    return segments.isEmpty();
  }
}
