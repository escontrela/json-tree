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
    boolean nightModeEnabled) {

  public LargePreviewSettingsSnapshot(long largePreviewThresholdBytes, int viewerChunkBytes) {
    this(largePreviewThresholdBytes, viewerChunkBytes, false, false);
  }

  public LargePreviewSettingsSnapshot(
      long largePreviewThresholdBytes,
      int viewerChunkBytes,
      boolean prettyOnLargePreviewEnabled) {
    this(largePreviewThresholdBytes, viewerChunkBytes, prettyOnLargePreviewEnabled, false);
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
  }

  public static LargePreviewSettingsSnapshot defaultsFrom(LargePreviewProperties properties) {
    return new LargePreviewSettingsSnapshot(
        sanitizeThreshold(properties.getFullRenderMaxBytes()),
        sanitizeViewerChunkBytes(properties.getVisibleChunkBytes()),
        false,
        false);
  }

  public LargePreviewSettingsSnapshot normalized() {
    return new LargePreviewSettingsSnapshot(
        sanitizeThreshold(largePreviewThresholdBytes),
        sanitizeViewerChunkBytes(viewerChunkBytes),
        prettyOnLargePreviewEnabled,
        nightModeEnabled);
  }

  private static long sanitizeThreshold(long thresholdBytes) {
    return Math.max(LargePreviewProperties.MIN_EDITABLE_BYTES, thresholdBytes);
  }

  private static int sanitizeViewerChunkBytes(int chunkBytes) {
    return Math.max(LargePreviewProperties.MIN_EDITABLE_BYTES, chunkBytes);
  }
}
