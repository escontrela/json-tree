package com.davidpe.jsontree.ui.model;

/**
 * UI-facing modal session descriptor for creating or editing one curl command before submission.
 */
public record CurlEditorSession(
    String title,
    String supportingText,
    String initialCommand,
    Runnable onSuccess) {

  public CurlEditorSession {
    title = title == null || title.isBlank() ? "Curl editor" : title;
    supportingText =
        supportingText == null || supportingText.isBlank()
            ? "Paste or type one supported curl command."
            : supportingText;
    initialCommand = initialCommand == null ? "" : initialCommand;
    onSuccess = onSuccess == null ? () -> {} : onSuccess;
  }

  public static CurlEditorSession empty(Runnable onSuccess) {
    return new CurlEditorSession(
        "New curl request",
        "Paste or type one supported curl command to create a new remote inspection.",
        "",
        onSuccess);
  }

  public static CurlEditorSession prefilled(String curlCommand, Runnable onSuccess) {
    return new CurlEditorSession(
        "Edit and rerun curl",
        "Adjust the stored curl command and run it again as a brand-new inspection entry.",
        curlCommand,
        onSuccess);
  }
}
