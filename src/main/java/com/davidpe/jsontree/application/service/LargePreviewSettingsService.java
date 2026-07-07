package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.in.SaveLargePreviewSettingsUseCase;
import com.davidpe.jsontree.application.port.in.ViewLargePreviewSettingsUseCase;
import com.davidpe.jsontree.application.port.out.LargePreviewSettingsStore;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Holds the runtime snapshot for the editable large-preview settings.
 *
 * <p>The service loads a persisted snapshot at startup, falls back to safe defaults when
 * persistence is absent or invalid, and later tickets reuse it to stage runtime updates from the
 * settings screen.
 */
@Service
public class LargePreviewSettingsService
    implements ViewLargePreviewSettingsUseCase, SaveLargePreviewSettingsUseCase {

  private final LargePreviewSettingsStore settingsStore;
  private final LargePreviewSettingsSnapshot defaultSnapshot;
  private volatile LargePreviewSettingsSnapshot currentSnapshot;

  @Autowired
  public LargePreviewSettingsService(
      LargePreviewSettingsStore settingsStore, LargePreviewProperties largePreviewProperties) {
    this.settingsStore = settingsStore;
    this.defaultSnapshot = LargePreviewSettingsSnapshot.defaultsFrom(largePreviewProperties);
    this.currentSnapshot = loadInitialSnapshot();
  }

  public LargePreviewSettingsService(
      LargePreviewSettingsStore settingsStore, LargePreviewSettingsSnapshot defaultSnapshot) {
    this.settingsStore = settingsStore;
    this.defaultSnapshot = defaultSnapshot.normalized();
    this.currentSnapshot = loadInitialSnapshot();
  }

  public LargePreviewSettingsSnapshot current() {
    return currentSnapshot;
  }

  @Override
  public LargePreviewSettingsSnapshot currentLargePreviewSettings() {
    return current();
  }

  public LargePreviewSettingsSnapshot defaults() {
    return defaultSnapshot;
  }

  public synchronized LargePreviewSettingsSnapshot reload() {
    currentSnapshot = loadInitialSnapshot();
    return currentSnapshot;
  }

  public synchronized LargePreviewSettingsSnapshot saveAndApply(
      LargePreviewSettingsSnapshot nextSnapshot) {
    LargePreviewSettingsSnapshot normalizedSnapshot = nextSnapshot.normalized();
    settingsStore.save(normalizedSnapshot);
    currentSnapshot = normalizedSnapshot;
    return currentSnapshot;
  }

  @Override
  public LargePreviewSettingsSnapshot saveLargePreviewSettings(
      LargePreviewSettingsSnapshot snapshot) {
    return saveAndApply(snapshot);
  }

  private LargePreviewSettingsSnapshot loadInitialSnapshot() {
    try {
      return settingsStore
          .load()
          .map(LargePreviewSettingsSnapshot::normalized)
          .orElse(defaultSnapshot);
    } catch (IllegalStateException exception) {
      return defaultSnapshot;
    }
  }
}
