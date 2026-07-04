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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonViewerWorkflowServiceTest {

    private final JsonViewerWorkflowService service = new JsonViewerWorkflowService(
            unusedValidationPort(),
            new InMemoryHistoryRepository(),
            unusedRendererPort(),
            inspectionModeResolver(1024L)
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
                path -> new AsciiTreeDocument("root", "root\n└─ id: 1", 2),
                inspectionModeResolver(1024L)
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
        assertEquals(com.davidpe.jsontree.application.model.JsonInspectionMode.FULL, result.inspectionMode());
        assertTrue(result.capabilities().rawJsonAvailable());
        assertTrue(result.capabilities().searchAvailable());
    }

    @Test
    void classifiesLargeFilesBeforeRendering() throws IOException {
        AtomicBoolean classifiedBeforeRender = new AtomicBoolean(false);
        JsonInspectionModeResolver resolver = new JsonInspectionModeResolver(properties(16L)) {
            @Override
            public com.davidpe.jsontree.application.model.JsonInspectionMode resolve(JsonImportResult importResult) {
                classifiedBeforeRender.set(true);
                return super.resolve(importResult);
            }
        };
        JsonViewerWorkflowService workflowService = new JsonViewerWorkflowService(
                path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null),
                new InMemoryHistoryRepository(),
                path -> {
                    assertTrue(classifiedBeforeRender.get());
                    return new AsciiTreeDocument("root", "root\n└─ id: 1", 2);
                },
                resolver
        );
        Path importedFile = Files.writeString(tempDir.resolve("large.json"), "{\"id\":1,\"payload\":\"01234567890123456789\"}");

        JsonViewerLoadResult result = workflowService.loadImportedFile(new JsonImportResult(
                importedFile,
                "large.json",
                128L,
                true,
                true,
                true,
                JsonDocumentSourceKind.LOCAL_FILE
        ));

        assertEquals(com.davidpe.jsontree.application.model.JsonInspectionMode.LARGE_PREVIEW, result.inspectionMode());
        assertFalse(result.capabilities().rawJsonAvailable());
        assertFalse(result.capabilities().searchAvailable());
    }

    @Test
    void classifiesHistoryReopenUsingStoredMetadataSize() throws IOException {
        InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
        ImportedJsonFile historyEntry = new ImportedJsonFile(
                "2026-07-04_10-00-00_large.json",
                "large.json",
                Instant.parse("2026-07-04T10:00:00Z"),
                128L,
                3,
                true,
                false
        );
        repository.entry = historyEntry;
        repository.storedJson = "{\"id\":1}";
        repository.storedJsonPath =
                Files.writeString(tempDir.resolve(historyEntry.storedName()), repository.storedJson);
        JsonViewerWorkflowService workflowService = new JsonViewerWorkflowService(
                unusedValidationPort(),
                repository,
                new TrackingRendererPort(),
                inspectionModeResolver(16L)
        );

        JsonViewerLoadResult result = workflowService.reopenHistoryEntry(historyEntry.storedName()).orElseThrow();

        assertEquals(com.davidpe.jsontree.application.model.JsonInspectionMode.LARGE_PREVIEW, result.inspectionMode());
        assertFalse(result.capabilities().outlineAvailable());
    }

    @Test
    void returnsEmptyWhenHistorySnapshotPathIsMissing() {
        InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
        repository.entry = new ImportedJsonFile(
                "2026-07-04_10-00-00_missing.json",
                "missing.json",
                Instant.parse("2026-07-04T10:00:00Z"),
                24L,
                3,
                true,
                false
        );
        JsonViewerWorkflowService workflowService = new JsonViewerWorkflowService(
                unusedValidationPort(),
                repository,
                path -> new AsciiTreeDocument("root", "root", 1),
                inspectionModeResolver(16L)
        );

        assertTrue(workflowService.reopenHistoryEntry(repository.entry.storedName()).isEmpty());
    }

    private JsonValidationPort unusedValidationPort() {
        return path -> new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
    }

    private AsciiTreeRendererPort unusedRendererPort() {
        return path -> null;
    }

    private JsonInspectionModeResolver inspectionModeResolver(long fullRenderMaxBytes) {
        return new JsonInspectionModeResolver(properties(fullRenderMaxBytes));
    }

    private com.davidpe.jsontree.infrastructure.config.LargePreviewProperties properties(long fullRenderMaxBytes) {
        com.davidpe.jsontree.infrastructure.config.LargePreviewProperties properties =
                new com.davidpe.jsontree.infrastructure.config.LargePreviewProperties();
        properties.setFullRenderMaxBytes(fullRenderMaxBytes);
        return properties;
    }

    private static final class InMemoryHistoryRepository implements JsonHistoryRepository {

        private ImportedJsonFile entry;
        private String storedJson;
        private Path storedJsonPath;

        @Override
        public List<ImportedJsonFile> findAll() {
            return List.of();
        }

        @Override
        public Optional<ImportedJsonFile> findByStoredName(String storedName) {
            return Optional.ofNullable(entry).filter(existing -> existing.storedName().equals(storedName));
        }

        @Override
        public Optional<Path> resolveStoredJsonPath(String storedName) {
            return entry != null && entry.storedName().equals(storedName)
                    ? Optional.ofNullable(storedJsonPath)
                    : Optional.empty();
        }

        @Override
        public Optional<String> readStoredJson(String storedName) {
            return entry != null && entry.storedName().equals(storedName)
                    ? Optional.ofNullable(storedJson)
                    : Optional.empty();
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

    private static final class TrackingRendererPort implements AsciiTreeRendererPort {

        @Override
        public AsciiTreeDocument render(Path jsonFilePath) {
            throw new AssertionError("Large-preview history reopen must not use full render path.");
        }

        @Override
        public AsciiTreeDocument renderLargePreview(Path jsonFilePath) {
            return new AsciiTreeDocument("root", "root\n├─ preview: true", 2);
        }
    }
}
