package com.davidpe.jsontree.infrastructure.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "json-tree.app-data")
public class AppDataProperties {

    private Path rootDirectory = Path.of("app-data");
    private String historyDirectoryName = "history";
    private String metadataFileName = "metadata.json";

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
}
