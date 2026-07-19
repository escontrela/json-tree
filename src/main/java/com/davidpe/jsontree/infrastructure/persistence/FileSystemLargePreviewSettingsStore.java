package com.davidpe.jsontree.infrastructure.persistence;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import com.davidpe.jsontree.application.port.out.LargePreviewSettingsStore;
import com.davidpe.jsontree.infrastructure.config.AppDataProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import org.springframework.stereotype.Repository;

/**
 * File-based persistence for the editable large-preview settings snapshot.
 */
@Repository
public class FileSystemLargePreviewSettingsStore implements LargePreviewSettingsStore {

  private static final String THRESHOLD_KEY = "largePreviewThresholdBytes";
  private static final String VIEWER_CHUNK_KEY = "viewerChunkBytes";
  private static final String PRETTY_LARGE_PREVIEW_KEY = "prettyOnLargePreviewEnabled";
  private static final String NIGHT_MODE_KEY = "nightModeEnabled";
  private static final String DEFAULT_CURL_USER_AGENT_KEY = "defaultCurlUserAgent";

  private final AppDataProperties appDataProperties;

  public FileSystemLargePreviewSettingsStore(AppDataProperties appDataProperties) {
    this.appDataProperties = appDataProperties;
  }

  @Override
  public Optional<LargePreviewSettingsSnapshot> load() {
    Path settingsPath = settingsPath();
    if (!Files.exists(settingsPath)) {
      return Optional.empty();
    }

    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(settingsPath)) {
      properties.load(inputStream);
      String thresholdValue = properties.getProperty(THRESHOLD_KEY);
      String viewerChunkValue = properties.getProperty(VIEWER_CHUNK_KEY);
      if (thresholdValue == null || viewerChunkValue == null) {
        return Optional.empty();
      }
      String prettyLargePreviewValue =
          properties.getProperty(PRETTY_LARGE_PREVIEW_KEY, Boolean.FALSE.toString());
      String nightModeValue = properties.getProperty(NIGHT_MODE_KEY, Boolean.FALSE.toString());
      String defaultCurlUserAgentValue =
          properties.getProperty(
              DEFAULT_CURL_USER_AGENT_KEY, LargePreviewSettingsSnapshot.DEFAULT_CURL_USER_AGENT);
      return Optional.of(
          new LargePreviewSettingsSnapshot(
              Long.parseLong(thresholdValue.trim()),
              Integer.parseInt(viewerChunkValue.trim()),
              defaultCurlUserAgentValue,
              Boolean.parseBoolean(prettyLargePreviewValue.trim()),
              Boolean.parseBoolean(nightModeValue.trim())));
    } catch (IOException | IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  @Override
  public void save(LargePreviewSettingsSnapshot snapshot) {
    try {
      Files.createDirectories(rootDirectory());
      Properties properties = new Properties();
      properties.setProperty(
          THRESHOLD_KEY, Long.toString(snapshot.largePreviewThresholdBytes()));
      properties.setProperty(VIEWER_CHUNK_KEY, Integer.toString(snapshot.viewerChunkBytes()));
      properties.setProperty(
          PRETTY_LARGE_PREVIEW_KEY, Boolean.toString(snapshot.prettyOnLargePreviewEnabled()));
      properties.setProperty(NIGHT_MODE_KEY, Boolean.toString(snapshot.nightModeEnabled()));
      properties.setProperty(DEFAULT_CURL_USER_AGENT_KEY, snapshot.defaultCurlUserAgent());
      try (OutputStream outputStream = Files.newOutputStream(settingsPath())) {
        properties.store(outputStream, "JSON TREE runtime settings");
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to persist large-preview settings.", exception);
    }
  }

  private Path rootDirectory() {
    return appDataProperties.getRootDirectory().toAbsolutePath().normalize();
  }

  private Path settingsPath() {
    return rootDirectory().resolve(appDataProperties.getLargePreviewSettingsFileName());
  }
}
