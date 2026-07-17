package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import org.springframework.stereotype.Component;

@Component
public class ViewerCapabilityPresentationResolver {

  public ViewerCapabilityPresentation resolve(JsonViewerLoadResult result) {
    if (result.usesLargePreview()) {
      return new ViewerCapabilityPresentation(
          false,
          false,
          false,
          "Copy preview",
          "Preview",
          "status-accent",
          " • byte-paged large preview",
          "Showing the current large-file page as a raw byte chunk",
          "PREVIEW",
          "Outline unavailable",
          "Large preview disables the outline and minimap while the viewer stays focused on the active page chunk.",
          "Large preview stays locked to the active page raw view. Regex search stays disabled.");
    }

    int renderedLines = result.hasRenderableTree() ? result.asciiTreeDocument().lineCount() : 0;
    return new ViewerCapabilityPresentation(
        result.capabilities().rawJsonAvailable(),
        result.capabilities().searchAvailable(),
        result.capabilities().outlineAvailable(),
        "Copy tree",
        "Valid",
        "status-valid",
        "",
        "Rendered " + renderedLines + " lines",
        "VALID",
        "JSON outline",
        "",
        "");
  }
}
