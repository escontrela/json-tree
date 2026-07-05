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
    @DisplayName("main FXML keeps the large outline as minimap shell instead of page-card rail")
    void mainFxmlDoesNotExposeLargeOutlinePageCardRail() throws IOException {
        String fxml = readResource("/com/davidpe/jsontree/ui/main.fxml");

        assertTrue(!fxml.contains("largePreviewOutlineScrollPane"));
        assertTrue(!fxml.contains("largePreviewOutlineStepsBox"));
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
