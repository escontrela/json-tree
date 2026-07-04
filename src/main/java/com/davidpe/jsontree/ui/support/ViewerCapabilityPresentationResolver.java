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
          true,
          "Copy preview",
          "Preview",
          "status-accent",
          " • bounded large preview",
          "Showing bounded large preview for oversized JSON",
          "PREVIEW",
          "Preview outline",
          "Large preview mode uses a bounded outline minimap derived from the visible preview.",
          "Raw JSON and regex search stay disabled until a smaller file is loaded.");
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
