package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.out.LargePreviewSettingsStore;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LargePreviewSettingsServiceTest {

  @Test
  void fallsBackToDefaultsWhenNoPersistedSnapshotExists() {
    LargePreviewSettingsService service =
        new LargePreviewSettingsService(emptyStore(), new LargePreviewSettingsSnapshot(2_048L, 4_096));

    assertEquals(new LargePreviewSettingsSnapshot(2_048L, 4_096), service.current());
  }

  @Test
  void prefersPersistedSnapshotWhenItIsValid() {
    LargePreviewSettingsService service =
        new LargePreviewSettingsService(
            storeReturning(new LargePreviewSettingsSnapshot(8_192L, 16_384)),
            new LargePreviewSettingsSnapshot(2_048L, 4_096));

    assertEquals(new LargePreviewSettingsSnapshot(8_192L, 16_384), service.current());
  }

  @Test
  void fallsBackToDefaultsWhenPersistedSnapshotIsInvalid() {
    LargePreviewSettingsService service =
        new LargePreviewSettingsService(
            storeThrowing(),
            new LargePreviewSettingsSnapshot(2_048L, 4_096));

    assertEquals(new LargePreviewSettingsSnapshot(2_048L, 4_096), service.current());
  }

  private LargePreviewSettingsStore emptyStore() {
    return new LargePreviewSettingsStore() {
      @Override
      public Optional<LargePreviewSettingsSnapshot> load() {
        return Optional.empty();
      }

      @Override
      public void save(LargePreviewSettingsSnapshot snapshot) {}
    };
  }

  private LargePreviewSettingsStore storeReturning(LargePreviewSettingsSnapshot snapshot) {
    return new LargePreviewSettingsStore() {
      @Override
      public Optional<LargePreviewSettingsSnapshot> load() {
        return Optional.of(snapshot);
      }

      @Override
      public void save(LargePreviewSettingsSnapshot ignored) {}
    };
  }

  private LargePreviewSettingsStore storeThrowing() {
    return new LargePreviewSettingsStore() {
      @Override
      public Optional<LargePreviewSettingsSnapshot> load() {
        throw new IllegalStateException("broken");
      }

      @Override
      public void save(LargePreviewSettingsSnapshot snapshot) {}
    };
  }
}
