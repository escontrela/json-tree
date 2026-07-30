package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import org.springframework.stereotype.Component;

/** Resolves editable settings-form state from runtime snapshot values and current text input. */
@Component
public class SettingsFormStateResolver {

  public SettingsFormState initialState(
      LargePreviewSettingsSnapshot snapshot, long startupMaxMemoryBytes) {
    return resolve(
        Long.toString(snapshot.largePreviewThresholdBytes()),
        Integer.toString(snapshot.viewerChunkBytes()),
        snapshot.defaultCurlUserAgent(),
        snapshot.prettyOnLargePreviewEnabled(),
        snapshot.nightModeEnabled(),
        startupMaxMemoryBytes);
  }

  public SettingsFormState resolve(
      String thresholdText,
      String chunkText,
      String defaultCurlUserAgentText,
      boolean prettyLargePreviewSelected,
      boolean nightModeSelected,
      long startupMaxMemoryBytes) {
    ParsedLong threshold = parsePositiveLong(thresholdText);
    ParsedInt chunk = parsePositiveInt(chunkText);
    ParsedText userAgent = parseRequiredText(defaultCurlUserAgentText);
    boolean warningActive =
        threshold.valid() && threshold.value() > 0L && threshold.value() > startupMaxMemoryBytes;
    return new SettingsFormState(
        safeText(thresholdText),
        safeText(chunkText),
        safeText(defaultCurlUserAgentText),
        prettyLargePreviewSelected,
        nightModeSelected,
        "Startup JVM reference: " + ByteSizeFormatter.format(startupMaxMemoryBytes),
        warningActive
            ? "Large preview threshold exceeds the startup JVM memory reference."
            : "Large preview threshold stays within the startup JVM memory reference.",
        warningActive,
        threshold.errorText(),
        chunk.errorText(),
        userAgent.errorText(),
        threshold.valid() && chunk.valid() && userAgent.valid());
  }

  private ParsedLong parsePositiveLong(String value) {
    if (value == null || value.isBlank()) {
      return ParsedLong.invalid("Enter a byte threshold.");
    }
    try {
      long parsed = Long.parseLong(value.trim());
      if (parsed < LargePreviewProperties.MIN_EDITABLE_BYTES) {
        return ParsedLong.invalid(
            "Threshold must be at least " + LargePreviewProperties.MIN_EDITABLE_BYTES + " bytes.");
      }
      return ParsedLong.valid(parsed);
    } catch (NumberFormatException exception) {
      return ParsedLong.invalid("Threshold must be a whole number.");
    }
  }

  private ParsedInt parsePositiveInt(String value) {
    if (value == null || value.isBlank()) {
      return ParsedInt.invalid("Enter a chunk size.");
    }
    try {
      int parsed = Integer.parseInt(value.trim());
      if (parsed < LargePreviewProperties.MIN_EDITABLE_BYTES) {
        return ParsedInt.invalid(
            "Chunk size must be at least " + LargePreviewProperties.MIN_EDITABLE_BYTES + " bytes.");
      }
      return ParsedInt.valid(parsed);
    } catch (NumberFormatException exception) {
      return ParsedInt.invalid("Chunk size must be a whole number.");
    }
  }

  private String safeText(String value) {
    return value == null ? "" : value;
  }

  private ParsedText parseRequiredText(String value) {
    if (value == null || value.isBlank()) {
      return ParsedText.invalid("Enter a default User-Agent.");
    }
    return ParsedText.valid(value.trim());
  }

  private record ParsedLong(boolean valid, long value, String errorText) {

    private static ParsedLong valid(long value) {
      return new ParsedLong(true, value, "");
    }

    private static ParsedLong invalid(String errorText) {
      return new ParsedLong(false, 0L, errorText);
    }
  }

  private record ParsedInt(boolean valid, int value, String errorText) {

    private static ParsedInt valid(int value) {
      return new ParsedInt(true, value, "");
    }

    private static ParsedInt invalid(String errorText) {
      return new ParsedInt(false, 0, errorText);
    }
  }

  private record ParsedText(boolean valid, String value, String errorText) {

    private static ParsedText valid(String value) {
      return new ParsedText(true, value, "");
    }

    private static ParsedText invalid(String errorText) {
      return new ParsedText(false, "", errorText);
    }
  }
}
