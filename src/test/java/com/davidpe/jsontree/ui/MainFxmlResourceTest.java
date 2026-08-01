package com.davidpe.jsontree.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MainFxmlResourceTest {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("<\\?import\\s+([\\w.]+)\\?>");
    private static final Pattern TYPE_PATTERN = Pattern.compile("<([A-Z][A-Za-z0-9_]*)\\b");

    @Test
    @DisplayName("main FXML resource is present on the classpath")
    void mainFxmlResourceIsPresentOnTheClasspath() {
        assertNotNull(
                MainFxmlResourceTest.class.getResource("/com/davidpe/jsontree/ui/main.fxml")
        );
    }

    @Test
    @DisplayName("history FXML resource is present on the classpath")
    void historyFxmlResourceIsPresentOnTheClasspath() {
        assertNotNull(
                MainFxmlResourceTest.class.getResource("/com/davidpe/jsontree/ui/history.fxml")
        );
    }

    @Test
    @DisplayName("settings FXML resource is present on the classpath")
    void settingsFxmlResourceIsPresentOnTheClasspath() {
        assertNotNull(
                MainFxmlResourceTest.class.getResource("/com/davidpe/jsontree/ui/settings.fxml")
        );
    }

    @Test
    @DisplayName("search panel FXML resource is present on the classpath")
    void searchPanelFxmlResourceIsPresentOnTheClasspath() {
        assertNotNull(
                MainFxmlResourceTest.class.getResource("/com/davidpe/jsontree/ui/controls/search/search-panel.fxml")
        );
    }

    @Test
    @DisplayName("main FXML imports every referenced JavaFX type")
    void mainFxmlImportsEveryReferencedJavaFxType() throws IOException {
        assertReferencedTypesAreImported("/com/davidpe/jsontree/ui/main.fxml");
    }

    @Test
    @DisplayName("history FXML imports every referenced JavaFX type")
    void historyFxmlImportsEveryReferencedJavaFxType() throws IOException {
        assertReferencedTypesAreImported("/com/davidpe/jsontree/ui/history.fxml");
    }

    @Test
    @DisplayName("settings FXML imports every referenced JavaFX type")
    void settingsFxmlImportsEveryReferencedJavaFxType() throws IOException {
        assertReferencedTypesAreImported("/com/davidpe/jsontree/ui/settings.fxml");
    }

    @Test
    @DisplayName("search panel FXML imports every referenced JavaFX type")
    void searchPanelFxmlImportsEveryReferencedJavaFxType() throws IOException {
        assertReferencedTypesAreImported("/com/davidpe/jsontree/ui/controls/search/search-panel.fxml");
    }

    @Test
    @DisplayName("main FXML keeps the large outline as minimap shell instead of page-card rail")
    void mainFxmlDoesNotExposeLargeOutlinePageCardRail() throws IOException {
        String fxml = readResource("/com/davidpe/jsontree/ui/main.fxml");

        assertTrue(!fxml.contains("largePreviewOutlineScrollPane"));
        assertTrue(!fxml.contains("largePreviewOutlineStepsBox"));
    }

    @Test
    @DisplayName("main FXML exposes the settings toolbar action")
    void mainFxmlExposesSettingsToolbarAction() throws IOException {
        String fxml = readResource("/com/davidpe/jsontree/ui/main.fxml");

        assertTrue(fxml.contains("onAction=\"#openSettings\""));
        assertTrue(fxml.contains("fx:id=\"settingsButton\""));
        assertTrue(fxml.contains("<ToolbarIconButton fx:id=\"settingsButton\""));
        assertTrue(fxml.contains("lightIconResource=\"/com/davidpe/jsontree/images/settings_35dp_000000.png\""));
        assertTrue(fxml.contains("darkIconResource=\"/com/davidpe/jsontree/images/settings_35dp_FFFFFF.png\""));
    }

    @Test
    @DisplayName("history and settings FXML use the close icon resources")
    void historyAndSettingsFxmlUseCloseIconResources() throws IOException {
        String historyFxml = readResource("/com/davidpe/jsontree/ui/history.fxml");
        String settingsFxml = readResource("/com/davidpe/jsontree/ui/settings.fxml");

        assertTrue(historyFxml.contains("/com/davidpe/jsontree/images/close_35dp_000000.png"));
        assertTrue(historyFxml.contains("/com/davidpe/jsontree/images/close_35dp_FFFFFF.png"));
        assertTrue(settingsFxml.contains("/com/davidpe/jsontree/images/close_35dp_000000.png"));
        assertTrue(settingsFxml.contains("/com/davidpe/jsontree/images/close_35dp_FFFFFF.png"));
        assertTrue(!historyFxml.contains("arrow_back_35dp_000000_FILL0_wght400_GRAD0_opsz40.png"));
        assertTrue(!settingsFxml.contains("arrow_back_35dp_FFFFFF_FILL0_wght400_GRAD0_opsz40.png"));
    }

    private static void assertReferencedTypesAreImported(String resourcePath) throws IOException {
        String fxml = readResource(resourcePath);
        Set<String> importedTypes = extractImportedTypes(fxml);
        Set<String> referencedTypes = extractReferencedTypes(fxml);
        Set<String> missingTypes = new LinkedHashSet<>(referencedTypes);
        missingTypes.removeAll(importedTypes);

        assertTrue(
                missingTypes.isEmpty(),
                () -> "FXML types without import in " + resourcePath + ": " + missingTypes
        );
    }

    private static String readResource(String resourcePath) throws IOException {
        try (InputStream stream = MainFxmlResourceTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "Resource not found: " + resourcePath);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Set<String> extractImportedTypes(String fxml) {
        Set<String> importedTypes = new LinkedHashSet<>();
        Matcher importMatcher = IMPORT_PATTERN.matcher(fxml);
        while (importMatcher.find()) {
            String qualifiedName = importMatcher.group(1);
            importedTypes.add(qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1));
        }
        return importedTypes;
    }

    private static Set<String> extractReferencedTypes(String fxml) {
        Set<String> referencedTypes = new LinkedHashSet<>();
        Matcher typeMatcher = TYPE_PATTERN.matcher(fxml);
        while (typeMatcher.find()) {
            referencedTypes.add(typeMatcher.group(1));
        }
        return referencedTypes;
    }
}
