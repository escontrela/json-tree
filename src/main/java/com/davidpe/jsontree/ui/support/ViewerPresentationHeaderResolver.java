package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.ui.model.ViewerPresentationMode;

/**
 * Resolves the main viewer header copy from the active presentation mode.
 */
public class ViewerPresentationHeaderResolver {

  public ViewerPresentationHeader resolve(ViewerPresentationMode presentationMode, boolean cropActive) {
    if (cropActive) {
      return new ViewerPresentationHeader("Crop view", "Search-derived JSON subset");
    }
    return switch (presentationMode) {
      case ASCII_TREE -> new ViewerPresentationHeader("ASCII viewer", "Structured developer output");
      case STRUCTURE -> new ViewerPresentationHeader("Structure view", "Merged JSON structure");
      case MARKDOWN_RENDERED -> new ViewerPresentationHeader("Markdown viewer", "Rendered Markdown reading view");
      case RAW_MARKDOWN -> new ViewerPresentationHeader("Raw Markdown", "Exact Markdown source");
      case RAW_JSON -> new ViewerPresentationHeader("Raw JSON", "Exact JSON source");
    };
  }

  public ViewerPresentationHeader nonRenderable(String titleText) {
    return new ViewerPresentationHeader("Viewer", titleText);
  }
}
