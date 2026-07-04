package com.davidpe.jsontree.ui.support;

public record SearchHighlightRange(
    int startIndex,
    int endIndex,
    boolean active
) {

  public SearchHighlightRange {
    if (startIndex < 0) {
      throw new IllegalArgumentException("Highlight start index must be zero or greater.");
    }
    if (endIndex < startIndex) {
      throw new IllegalArgumentException("Highlight end index must not be before the start index.");
    }
  }
}
