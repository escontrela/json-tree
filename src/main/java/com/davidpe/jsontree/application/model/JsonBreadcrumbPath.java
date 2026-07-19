package com.davidpe.jsontree.application.model;

import java.util.ArrayList;
import java.util.List;

public record JsonBreadcrumbPath(List<JsonBreadcrumbSegment> segments) {

  public JsonBreadcrumbPath {
    segments = List.copyOf(segments);
    if (segments.isEmpty()) {
      throw new IllegalArgumentException("Breadcrumb path requires at least one segment.");
    }
  }

  public static JsonBreadcrumbPath root() {
    return new JsonBreadcrumbPath(List.of(JsonBreadcrumbSegment.root()));
  }

  public JsonBreadcrumbPath appendProperty(String propertyName) {
    ArrayList<JsonBreadcrumbSegment> nextSegments = new ArrayList<>(segments);
    nextSegments.add(JsonBreadcrumbSegment.property(propertyName));
    return new JsonBreadcrumbPath(nextSegments);
  }

  public JsonBreadcrumbPath appendArrayIndex(int index) {
    ArrayList<JsonBreadcrumbSegment> nextSegments = new ArrayList<>(segments);
    nextSegments.add(JsonBreadcrumbSegment.arrayIndex(index));
    return new JsonBreadcrumbPath(nextSegments);
  }

  public String displayLabel() {
    return segments.stream().map(JsonBreadcrumbSegment::displayLabel).collect(java.util.stream.Collectors.joining(" / "));
  }
}
