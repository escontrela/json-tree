package com.davidpe.jsontree.infrastructure.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "json-tree.app-data")
public class AppDataProperties {

    private Path rootDirectory = Path.of("app-data");
    private String historyDirectoryName = "history";
    private String metadataFileName = "metadata.json";
    private String largePreviewSettingsFileName = "large-preview-settings.properties";

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getHistoryDirectoryName() {
        return historyDirectoryName;
    }

    public void setHistoryDirectoryName(String historyDirectoryName) {
        this.historyDirectoryName = historyDirectoryName;
    }

    public String getMetadataFileName() {
        return metadataFileName;
    }

    public void setMetadataFileName(String metadataFileName) {
        this.metadataFileName = metadataFileName;
    }

    public String getLargePreviewSettingsFileName() {
        return largePreviewSettingsFileName;
    }

    public void setLargePreviewSettingsFileName(String largePreviewSettingsFileName) {
        this.largePreviewSettingsFileName = largePreviewSettingsFileName;
    }
}
