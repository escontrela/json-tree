package com.davidpe.jsontree.application.model;

public record JsonOutlineEntry(
    int depth,
    int visualWeight,
    JsonOutlineEntryKind kind,
    int childCount
) {

  public JsonOutlineEntry {
    if (depth < 0) {
      throw new IllegalArgumentException("Outline depth must be zero or greater.");
    }
    if (visualWeight < 1) {
      throw new IllegalArgumentException("Outline visual weight must be at least one.");
    }
    if (childCount < 0) {
      throw new IllegalArgumentException("Outline child count must be zero or greater.");
    }
  }

  public boolean container() {
    return kind != JsonOutlineEntryKind.VALUE;
  }
}
