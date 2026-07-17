package com.davidpe.jsontree.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppDataPropertiesTest {

    @Test
    @DisplayName("default app-data conventions match the project requirements")
    void defaultAppDataConventionsMatchProjectRequirements() {
        AppDataProperties properties = new AppDataProperties();

        assertEquals(Path.of("app-data"), properties.getRootDirectory());
        assertEquals("history", properties.getHistoryDirectoryName());
        assertEquals("metadata.json", properties.getMetadataFileName());
        assertEquals(
                "large-preview-settings.properties",
                properties.getLargePreviewSettingsFileName());
    }

    @Test
    @DisplayName("custom app-data configuration can be overridden explicitly")
    void customAppDataConfigurationCanBeOverriddenExplicitly() {
        AppDataProperties properties = new AppDataProperties();

        properties.setRootDirectory(Path.of("custom-data"));
        properties.setHistoryDirectoryName("imports");
        properties.setMetadataFileName("history-metadata.json");
        properties.setLargePreviewSettingsFileName("preview.properties");

        assertEquals(Path.of("custom-data"), properties.getRootDirectory());
        assertEquals("imports", properties.getHistoryDirectoryName());
        assertEquals("history-metadata.json", properties.getMetadataFileName());
        assertEquals("preview.properties", properties.getLargePreviewSettingsFileName());
    }
}
