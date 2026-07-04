package com.davidpe.jsontree.application.model;

public record JsonSearchMatch(
    int startIndex,
    int endIndex,
    String fragment
) {

  public JsonSearchMatch {
    if (startIndex < 0) {
      throw new IllegalArgumentException("Search match start index must be zero or greater.");
    }
    if (endIndex < startIndex) {
      throw new IllegalArgumentException("Search match end index must not be before start index.");
    }
  }

  public int length() {
    return endIndex - startIndex;
  }
}
