package com.davidpe.jsontree.application.model;

import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;

/**
 * Runtime-editable subset of large-preview configuration.
 *
 * <p>The snapshot is intentionally small and transport-friendly so application services can share
 * it without leaking JavaFX or persistence details.
 */
public record LargePreviewSettingsSnapshot(
    long largePreviewThresholdBytes,
    int viewerChunkBytes,
    boolean prettyOnLargePreviewEnabled,
    boolean nightModeEnabled,
    String defaultCurlUserAgent) {

  public static final String DEFAULT_CURL_USER_AGENT =
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

  public LargePreviewSettingsSnapshot(long largePreviewThresholdBytes, int viewerChunkBytes) {
    this(largePreviewThresholdBytes, viewerChunkBytes, false, false, DEFAULT_CURL_USER_AGENT);
  }

  public LargePreviewSettingsSnapshot(
      long largePreviewThresholdBytes,
      int viewerChunkBytes,
      boolean prettyOnLargePreviewEnabled) {
    this(
        largePreviewThresholdBytes,
        viewerChunkBytes,
        prettyOnLargePreviewEnabled,
        false,
        DEFAULT_CURL_USER_AGENT);
  }

  public LargePreviewSettingsSnapshot(
      long largePreviewThresholdBytes,
      int viewerChunkBytes,
      String defaultCurlUserAgent,
      boolean prettyOnLargePreviewEnabled,
      boolean nightModeEnabled) {
    this(
        largePreviewThresholdBytes,
        viewerChunkBytes,
        prettyOnLargePreviewEnabled,
        nightModeEnabled,
        defaultCurlUserAgent);
  }

  public LargePreviewSettingsSnapshot(
      long largePreviewThresholdBytes,
      int viewerChunkBytes,
      boolean prettyOnLargePreviewEnabled,
      boolean nightModeEnabled) {
    this(
        largePreviewThresholdBytes,
        viewerChunkBytes,
        prettyOnLargePreviewEnabled,
        nightModeEnabled,
        DEFAULT_CURL_USER_AGENT);
  }

  public LargePreviewSettingsSnapshot {
    if (largePreviewThresholdBytes < 1L) {
      throw new IllegalArgumentException(
          "Large-preview threshold bytes must be greater than zero.");
    }
    if (viewerChunkBytes < LargePreviewProperties.MIN_EDITABLE_BYTES) {
      throw new IllegalArgumentException(
          "Large-preview viewer chunk bytes must respect the minimum byte limit.");
    }
    defaultCurlUserAgent = sanitizeUserAgent(defaultCurlUserAgent);
  }

  public static LargePreviewSettingsSnapshot defaultsFrom(LargePreviewProperties properties) {
    return new LargePreviewSettingsSnapshot(
        sanitizeThreshold(properties.getFullRenderMaxBytes()),
        sanitizeViewerChunkBytes(properties.getVisibleChunkBytes()),
        false,
        false,
        DEFAULT_CURL_USER_AGENT);
  }

  public LargePreviewSettingsSnapshot normalized() {
    return new LargePreviewSettingsSnapshot(
        sanitizeThreshold(largePreviewThresholdBytes),
        sanitizeViewerChunkBytes(viewerChunkBytes),
        prettyOnLargePreviewEnabled,
        nightModeEnabled,
        defaultCurlUserAgent);
  }

  private static long sanitizeThreshold(long thresholdBytes) {
    return Math.max(LargePreviewProperties.MIN_EDITABLE_BYTES, thresholdBytes);
  }

  private static int sanitizeViewerChunkBytes(int chunkBytes) {
    return Math.max(LargePreviewProperties.MIN_EDITABLE_BYTES, chunkBytes);
  }

  private static String sanitizeUserAgent(String userAgent) {
    if (userAgent == null || userAgent.isBlank()) {
      return DEFAULT_CURL_USER_AGENT;
    }
    return userAgent.trim();
  }
}
