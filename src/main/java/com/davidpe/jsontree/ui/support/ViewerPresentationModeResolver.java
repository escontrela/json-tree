package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;

/**
 * Resolves the effective viewer presentation mode for the currently loaded document.
 *
 * <p>The resolver keeps document-kind rules centralized so controllers do not need to coordinate
 * ad hoc booleans for Markdown, JSON, and large-preview behavior.
 */
public final class ViewerPresentationModeResolver {

  public ViewerPresentationMode resolve(
      ViewerPresentationMode requestedMode, JsonViewerLoadResult result) {
    if (result == null) {
      return ViewerPresentationMode.ASCII_TREE;
    }
    if (result.usesLargePreview()) {
      return result.markdownDocument()
          ? ViewerPresentationMode.RAW_MARKDOWN
          : ViewerPresentationMode.RAW_JSON;
    }
    if (result.markdownDocument()) {
      return requestedMode == ViewerPresentationMode.RAW_MARKDOWN
          ? ViewerPresentationMode.RAW_MARKDOWN
          : ViewerPresentationMode.MARKDOWN_RENDERED;
    }
    if (requestedMode == ViewerPresentationMode.RAW_JSON
        && result.capabilities().rawJsonAvailable()) {
      return ViewerPresentationMode.RAW_JSON;
    }
    if (requestedMode == ViewerPresentationMode.STRUCTURE && result.hasRenderableTree()) {
      return ViewerPresentationMode.STRUCTURE;
    }
    return ViewerPresentationMode.ASCII_TREE;
  }
}
