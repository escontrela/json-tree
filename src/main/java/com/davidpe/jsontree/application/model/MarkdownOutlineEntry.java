package com.davidpe.jsontree.application.model;

/**
 * A compact Markdown outline entry anchored to a source line in the raw document.
 *
 * @param title heading title or fallback line label.
 * @param depth zero-based logical depth.
 * @param sourceLineIndex zero-based source line anchor.
 * @param visualWeight compact weight used by the minimap shell.
 * @param fallback whether the entry comes from the heading-less fallback path.
 */
public record MarkdownOutlineEntry(
    String title,
    int depth,
    int sourceLineIndex,
    int visualWeight,
    boolean fallback) {

  public MarkdownOutlineEntry {
    title = title == null ? "" : title.trim();
    if (depth < 0) {
      throw new IllegalArgumentException("Markdown outline depth must be zero or greater.");
    }
    if (sourceLineIndex < 0) {
      throw new IllegalArgumentException("Markdown outline source line must be zero or greater.");
    }
    if (visualWeight < 1) {
      throw new IllegalArgumentException("Markdown outline visual weight must be at least one.");
    }
  }

  public boolean heading() {
    return !fallback;
  }
}
