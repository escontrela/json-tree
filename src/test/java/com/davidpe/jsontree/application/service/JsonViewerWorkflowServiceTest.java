package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.JsonImportResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonViewerWorkflowServiceTest {

    private final JsonViewerWorkflowService service = new JsonViewerWorkflowService();

    @TempDir
    Path tempDir;

    @Test
    void importsAvailableFileMetadata() throws IOException {
        Path jsonFile = Files.writeString(tempDir.resolve("sample.json"), "{\n  \"name\": \"json-tree\"\n}");

        JsonImportResult result = service.importFile(jsonFile);

        assertNotNull(result.path());
        assertEquals(jsonFile.toAbsolutePath().normalize(), result.path());
        assertEquals("sample.json", result.fileName());
        assertEquals(Files.size(jsonFile), result.sizeBytes());
        assertTrue(result.exists());
        assertTrue(result.readable());
        assertTrue(result.regularFile());
        assertTrue(result.available());
    }

    @Test
    void reportsUnavailablePathWithoutThrowing() {
        Path missingFile = tempDir.resolve("missing.json");

        JsonImportResult result = service.importFile(missingFile);

        assertEquals(missingFile.toAbsolutePath().normalize(), result.path());
        assertEquals("missing.json", result.fileName());
        assertEquals(0L, result.sizeBytes());
        assertFalse(result.exists());
        assertFalse(result.available());
        assertFalse(result.regularFile());
    }
}
