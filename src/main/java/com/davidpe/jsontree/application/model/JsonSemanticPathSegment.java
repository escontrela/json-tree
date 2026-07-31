package com.davidpe.jsontree.application.model;

/**
 * Single segment inside a semantic JSON path.
 */
public record JsonSemanticPathSegment(String propertyName, Integer arrayIndex) {

  public JsonSemanticPathSegment {
    if ((propertyName == null) == (arrayIndex == null)) {
      throw new IllegalArgumentException("A path segment must be either a property or an array index.");
    }
  }

  public static JsonSemanticPathSegment property(String propertyName) {
    if (propertyName == null || propertyName.isBlank()) {
      throw new IllegalArgumentException("Property name must not be blank.");
    }
    return new JsonSemanticPathSegment(propertyName, null);
  }

  public static JsonSemanticPathSegment arrayIndex(int arrayIndex) {
    if (arrayIndex < 0) {
      throw new IllegalArgumentException("Array index must not be negative.");
    }
    return new JsonSemanticPathSegment(null, arrayIndex);
  }

  public boolean propertySegment() {
    return propertyName != null;
  }

  public boolean arrayIndexSegment() {
    return arrayIndex != null;
  }
}
