package com.davidpe.jsontree.ui.model;

/**
 * Supported presentation modes for the shared JSON viewers.
 */
public enum ViewerPresentationMode {
  ASCII_TREE,
  RAW_JSON,
  STRUCTURE,
  MARKDOWN_RENDERED,
  RAW_MARKDOWN;

  public boolean rawTextMode() {
    return this == RAW_JSON || this == RAW_MARKDOWN;
  }

  public boolean markdownMode() {
    return this == MARKDOWN_RENDERED || this == RAW_MARKDOWN;
  }
}
