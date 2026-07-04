package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonViewerWorkflowServiceTest {

    private final JsonViewerWorkflowService service = new JsonViewerWorkflowService(
            unusedValidationPort(),
            new InMemoryHistoryRepository(),
            unusedRendererPort()
    );

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

    @Test
    void loadsImportedFileUsingExistingImportResultObject() throws IOException {
        JsonViewerWorkflowService workflowService = new JsonViewerWorkflowService(
                path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
                new InMemoryHistoryRepository(),
                path -> new AsciiTreeDocument("root", "root\n└─ id: 1", 2)
        );
        Path importedFile = Files.writeString(tempDir.resolve("imported.json"), "{\"id\":1}");

        JsonViewerLoadResult result = workflowService.loadImportedFile(new JsonImportResult(
                importedFile,
                "imported.json",
                16L,
                true,
                true,
                true,
                JsonDocumentSourceKind.LOCAL_FILE
        ));

        assertTrue(result.validationResult().valid());
        assertTrue(result.hasRenderableTree());
        assertEquals("imported.json", result.importResult().fileName());
    }

    private JsonValidationPort unusedValidationPort() {
        return path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
    }

    private AsciiTreeRendererPort unusedRendererPort() {
        return path -> null;
    }

    private static final class InMemoryHistoryRepository implements JsonHistoryRepository {

        @Override
        public List<ImportedJsonFile> findAll() {
            return List.of();
        }

        @Override
        public Optional<ImportedJsonFile> findByStoredName(String storedName) {
            return Optional.empty();
        }

        @Override
        public Optional<String> readStoredJson(String storedName) {
            return Optional.empty();
        }

        @Override
        public void save(ImportedJsonFile importedJsonFile, String jsonContent) {
        }

        @Override
        public Optional<ImportedJsonFile> updateFavorite(String storedName, boolean favorite) {
            return Optional.empty();
        }

        @Override
        public void deleteByStoredName(String storedName) {
        }
    }
}
