package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import org.springframework.stereotype.Component;

@Component
public class ViewerCapabilityPresentationResolver {

  public ViewerCapabilityPresentation resolve(JsonViewerLoadResult result) {
    if (result.usesLargePreview()) {
      return new ViewerCapabilityPresentation(
          true,
          false,
          false,
          "Copy preview",
          "Preview",
          "status-accent",
          " • byte-paged large preview",
          "Showing a byte-bounded preview chunk for oversized JSON",
          "PREVIEW",
          "Outline unavailable",
          "Large preview disables the outline and minimap while the viewer pages through source chunks.",
          "Raw JSON stays available for the current chunk only. Regex search stays disabled.");
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
