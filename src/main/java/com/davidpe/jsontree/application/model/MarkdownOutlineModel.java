package com.davidpe.jsontree.application.model;

import java.util.List;

/**
 * Dedicated Markdown outline model used to keep heading anchors and fallback navigation metadata.
 *
 * @param entries ordered outline entries.
 * @param maxDepth deepest entry depth.
 * @param headingDriven whether the outline was built from headings instead of fallback anchors.
 * @param totalLines total source lines considered by the model.
 */
public record MarkdownOutlineModel(
    List<MarkdownOutlineEntry> entries,
    int maxDepth,
    boolean headingDriven,
    int totalLines) {

  public MarkdownOutlineModel {
    entries = List.copyOf(entries);
    if (maxDepth < 0) {
      throw new IllegalArgumentException("Markdown outline max depth must be zero or greater.");
    }
    if (totalLines < 0) {
      throw new IllegalArgumentException("Markdown outline total lines must be zero or greater.");
    }
  }

  public static MarkdownOutlineModel empty() {
    return new MarkdownOutlineModel(List.of(), 0, false, 0);
  }

  public boolean emptyModel() {
    return entries.isEmpty();
  }
}
