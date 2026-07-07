package com.davidpe.jsontree.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.infrastructure.config.AppDataProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemLargePreviewSettingsStoreTest {

  @TempDir Path tempDir;

  @Test
  void returnsEmptyWhenSettingsFileDoesNotExist() {
    FileSystemLargePreviewSettingsStore store = new FileSystemLargePreviewSettingsStore(properties());

    assertTrue(store.load().isEmpty());
  }

  @Test
  void savesAndLoadsSettingsRoundTrip() {
    FileSystemLargePreviewSettingsStore store = new FileSystemLargePreviewSettingsStore(properties());
    LargePreviewSettingsSnapshot snapshot =
        new LargePreviewSettingsSnapshot(8_388_608L, 262_144, true);

    store.save(snapshot);

    assertEquals(snapshot, store.load().orElseThrow());
  }

  @Test
  void persistedSettingsRemainAvailableForANewStoreInstance() {
    LargePreviewSettingsSnapshot snapshot =
        new LargePreviewSettingsSnapshot(8_388_608L, 262_144, true);

    new FileSystemLargePreviewSettingsStore(properties()).save(snapshot);

    assertEquals(snapshot, new FileSystemLargePreviewSettingsStore(properties()).load().orElseThrow());
  }

  @Test
  void oldSettingsFilesWithoutPrettyFlagRemainReadable() throws Exception {
    FileSystemLargePreviewSettingsStore store = new FileSystemLargePreviewSettingsStore(properties());
    Files.createDirectories(tempDir);
    Files.writeString(
        tempDir.resolve("preview.properties"),
        """
        largePreviewThresholdBytes=8388608
        viewerChunkBytes=262144
        """);

    assertEquals(
        new LargePreviewSettingsSnapshot(8_388_608L, 262_144, false), store.load().orElseThrow());
  }

  @Test
  void fallsBackToEmptyWhenPersistedValuesAreInvalid() throws Exception {
    FileSystemLargePreviewSettingsStore store = new FileSystemLargePreviewSettingsStore(properties());
    Files.createDirectories(tempDir);
    Files.writeString(
        tempDir.resolve("preview.properties"),
        """
        largePreviewThresholdBytes=broken
        viewerChunkBytes=still-broken
        """);

    assertTrue(store.load().isEmpty());
  }

  private AppDataProperties properties() {
    AppDataProperties properties = new AppDataProperties();
    properties.setRootDirectory(tempDir);
    properties.setLargePreviewSettingsFileName("preview.properties");
    return properties;
  }
}
