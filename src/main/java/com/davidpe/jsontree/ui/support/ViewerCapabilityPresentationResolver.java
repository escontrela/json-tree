package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.domain.model.DocumentFormat;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import org.springframework.stereotype.Component;

@Component
public class ViewerCapabilityPresentationResolver {

  public ViewerCapabilityPresentation resolve(
      JsonViewerLoadResult result, ViewerPresentationMode presentationMode) {
    DocumentFormat documentFormat = result.importResult().documentFormat();
    if (result.usesLargePreview()) {
      return new ViewerCapabilityPresentation(
          false,
          false,
          false,
          false,
          "Copy preview",
          "Preview",
          "status-accent",
          documentFormat.markdown()
              ? " • byte-paged large preview • markdown"
              : " • byte-paged large preview",
          withCurlRequest(
              result,
              documentFormat.markdown()
                  ? "Showing the current large-file page as a raw Markdown chunk"
                  : "Showing the current large-file page as a raw byte chunk"),
          "PREVIEW",
          "Outline unavailable",
          "Large preview disables the outline and minimap while the viewer stays focused on the active page chunk.",
          "Large preview stays locked to the active page raw view. Regex search stays disabled.");
    }

    if (documentFormat.markdown()) {
      int renderedLines = result.asciiTreeDocument() == null ? 0 : result.asciiTreeDocument().lineCount();
      return new ViewerCapabilityPresentation(
          false,
          false,
          result.capabilities().searchAvailable(),
          result.capabilities().outlineAvailable(),
          "Copy raw",
          "Markdown",
          "status-accent",
          "",
          withCurlRequest(result, "Rendered " + renderedLines + " markdown lines"),
          "MARKDOWN",
          "Markdown outline",
          "The Markdown outline follows heading anchors when available and falls back to source segments otherwise.",
          "Markdown stays in raw-source mode. Structure stays unavailable while regex search and outline remain active.");
    }

    int renderedLines = result.hasRenderableTree() ? result.asciiTreeDocument().lineCount() : 0;
    boolean structureActive = presentationMode == ViewerPresentationMode.STRUCTURE;
    return new ViewerCapabilityPresentation(
        result.capabilities().rawJsonAvailable(),
        result.hasRenderableTree(),
        !structureActive && result.capabilities().searchAvailable(),
        !structureActive && result.capabilities().outlineAvailable(),
        "Copy tree",
        "Valid",
        "status-valid",
        "",
        withCurlRequest(result, "Rendered " + renderedLines + " lines"),
        "VALID",
        "JSON outline",
        "",
        "");
  }

  private String withCurlRequest(JsonViewerLoadResult result, String baseStatus) {
    if (result.historyEntry() == null || !result.historyEntry().curlBacked()) {
      return baseStatus;
    }
    String curlCommand = result.historyEntry().curlCommand();
    if (curlCommand == null || curlCommand.isBlank()) {
      return baseStatus;
    }
    return baseStatus + " • Request: " + curlCommand;
  }
}
