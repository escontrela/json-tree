package com.davidpe.jsontree.application.model;

public record JsonBreadcrumbSegment(JsonBreadcrumbSegmentKind kind, String value) {

  public JsonBreadcrumbSegment {
    if (kind == null) {
      throw new IllegalArgumentException("Breadcrumb segment kind is required.");
    }
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Breadcrumb segment value is required.");
    }
  }

  public static JsonBreadcrumbSegment root() {
    return new JsonBreadcrumbSegment(JsonBreadcrumbSegmentKind.ROOT, "root");
  }

  public static JsonBreadcrumbSegment property(String propertyName) {
    return new JsonBreadcrumbSegment(JsonBreadcrumbSegmentKind.PROPERTY, propertyName);
  }

  public static JsonBreadcrumbSegment arrayIndex(int index) {
    return new JsonBreadcrumbSegment(JsonBreadcrumbSegmentKind.ARRAY_INDEX, Integer.toString(index));
  }

  public String displayLabel() {
    return kind == JsonBreadcrumbSegmentKind.ARRAY_INDEX ? "[" + value + "]" : value;
  }
}
