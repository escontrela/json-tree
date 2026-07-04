package com.davidpe.jsontree.infrastructure.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JacksonJsonValidationServiceTest {

    private final JacksonJsonValidationService service = new JacksonJsonValidationService(new ObjectMapper());

    @TempDir
    Path tempDir;

    @Test
    void validatesWellFormedJson() throws IOException {
        Path jsonFile = Files.writeString(tempDir.resolve("valid.json"), """
                {
                  "name": "json-tree",
                  "active": true
                }
                """);

        JsonValidationResult result = service.validate(jsonFile);

        assertEquals(JsonValidationStatus.VALID, result.status());
        assertTrue(result.valid());
        assertEquals("Valid JSON.", result.message());
        assertNull(result.line());
        assertNull(result.column());
    }

    @Test
    void reportsInvalidJsonWithLocation() throws IOException {
        Path jsonFile = Files.writeString(tempDir.resolve("invalid.json"), """
                {
                  "name":
                }
                """);

        JsonValidationResult result = service.validate(jsonFile);

        assertEquals(JsonValidationStatus.INVALID, result.status());
        assertFalse(result.valid());
        assertNotNull(result.message());
        assertTrue(result.message().startsWith("Invalid JSON:"));
        assertEquals(3, result.line());
        assertEquals(1, result.column());
    }

    @Test
    void reportsEmptyFilesExplicitly() throws IOException {
        Path jsonFile = Files.writeString(tempDir.resolve("empty.json"), "   ");

        JsonValidationResult result = service.validate(jsonFile);

        assertEquals(JsonValidationStatus.EMPTY, result.status());
        assertFalse(result.valid());
        assertEquals("File is empty.", result.message());
    }

    @Test
    void reportsParsingErrorsForUnreadablePaths() {
        Path missingFile = tempDir.resolve("missing.json");

        JsonValidationResult result = service.validate(missingFile);

        assertEquals(JsonValidationStatus.PARSING_ERROR, result.status());
        assertFalse(result.valid());
        assertTrue(result.message().startsWith("Unable to read JSON file:"));
    }

    @Test
    void validatesLargeJsonThroughStreamingPath() throws IOException {
        Path jsonFile = Files.writeString(
                tempDir.resolve("large-valid.json"),
                "{\"payload\":\"0123456789abcdef0123456789abcdef\",\"active\":true}");

        JsonValidationResult result = serviceWithThreshold(8L).validate(jsonFile);

        assertEquals(JsonValidationStatus.VALID, result.status());
        assertTrue(result.valid());
    }

    @Test
    void reportsInvalidLargeJsonWithStreamingLocation() throws IOException {
        Path jsonFile = Files.writeString(
                tempDir.resolve("large-invalid.json"),
                "{\n  \"payload\": [1, 2,\n}\n0123456789");

        JsonValidationResult result = serviceWithThreshold(8L).validate(jsonFile);

        assertEquals(JsonValidationStatus.INVALID, result.status());
        assertFalse(result.valid());
        assertTrue(result.message().startsWith("Invalid JSON:"));
        assertEquals(3, result.line());
        assertNotNull(result.column());
    }

    @Test
    void reportsWhitespaceOnlyLargeFilesAsEmpty() throws IOException {
        Path jsonFile = Files.writeString(tempDir.resolve("large-empty.json"), "                  ");

        JsonValidationResult result = serviceWithThreshold(8L).validate(jsonFile);

        assertEquals(JsonValidationStatus.EMPTY, result.status());
        assertFalse(result.valid());
        assertEquals("File is empty.", result.message());
    }

    private JacksonJsonValidationService serviceWithThreshold(long fullRenderMaxBytes) {
        LargePreviewProperties properties = new LargePreviewProperties();
        properties.setFullRenderMaxBytes(fullRenderMaxBytes);
        return new JacksonJsonValidationService(new ObjectMapper(), properties);
    }
}
