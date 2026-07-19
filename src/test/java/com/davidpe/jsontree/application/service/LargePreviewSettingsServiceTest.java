package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

  @Test
  void saveAndApplyPersistsAndUpdatesRuntimeSnapshot() {
    RecordingStore store = new RecordingStore(Optional.empty());
    LargePreviewSettingsService service =
        new LargePreviewSettingsService(store, new LargePreviewSettingsSnapshot(2_048L, 4_096));
    LargePreviewSettingsSnapshot updatedSnapshot =
        new LargePreviewSettingsSnapshot(8_192L, 16_384, true, true);

    LargePreviewSettingsSnapshot savedSnapshot = service.saveAndApply(updatedSnapshot);

    assertEquals(updatedSnapshot, savedSnapshot);
    assertEquals(updatedSnapshot, service.current());
    assertEquals(updatedSnapshot, store.savedSnapshot());
  }

  @Test
  void reloadReflectsPersistedSettingsForANewRuntimeWindow() {
    RecordingStore store =
        new RecordingStore(Optional.of(new LargePreviewSettingsSnapshot(2_048L, 4_096)));
    LargePreviewSettingsService service =
        new LargePreviewSettingsService(store, new LargePreviewSettingsSnapshot(1_024L, 2_048));
    LargePreviewSettingsSnapshot updatedSnapshot =
        new LargePreviewSettingsSnapshot(8_192L, 16_384);

    store.persistedSnapshot = Optional.of(updatedSnapshot);
    LargePreviewSettingsSnapshot reloadedSnapshot = service.reload();

    assertEquals(updatedSnapshot, reloadedSnapshot);
    assertSame(reloadedSnapshot, service.current());
  }

  @Test
  void defaultsNightModeToDisabledWhenLegacyConstructorsAreUsed() {
    LargePreviewSettingsSnapshot snapshot = new LargePreviewSettingsSnapshot(2_048L, 4_096, true);

    assertEquals(new LargePreviewSettingsSnapshot(2_048L, 4_096, true, false), snapshot);
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

  private static final class RecordingStore implements LargePreviewSettingsStore {

    private Optional<LargePreviewSettingsSnapshot> persistedSnapshot;
    private LargePreviewSettingsSnapshot savedSnapshot;

    private RecordingStore(Optional<LargePreviewSettingsSnapshot> persistedSnapshot) {
      this.persistedSnapshot = persistedSnapshot;
    }

    @Override
    public Optional<LargePreviewSettingsSnapshot> load() {
      return persistedSnapshot;
    }

    @Override
    public void save(LargePreviewSettingsSnapshot snapshot) {
      savedSnapshot = snapshot;
      persistedSnapshot = Optional.of(snapshot);
    }

    private LargePreviewSettingsSnapshot savedSnapshot() {
      return savedSnapshot;
    }
  }
}
