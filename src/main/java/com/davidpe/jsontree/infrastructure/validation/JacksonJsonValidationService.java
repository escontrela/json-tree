package com.davidpe.jsontree.infrastructure.validation;

import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JacksonJsonValidationService implements JsonValidationPort {

    private static final String EMPTY_FILE_MESSAGE = "File is empty.";

    private final ObjectMapper objectMapper;
    private final LargePreviewProperties largePreviewProperties;

    public JacksonJsonValidationService(ObjectMapper objectMapper) {
        this(objectMapper, new LargePreviewProperties());
    }

    @Autowired
    public JacksonJsonValidationService(
            ObjectMapper objectMapper,
            LargePreviewProperties largePreviewProperties) {
        this.objectMapper = objectMapper;
        this.largePreviewProperties = largePreviewProperties;
    }

    @Override
    public JsonValidationResult validate(Path jsonFilePath) {
        Path normalizedPath = jsonFilePath.toAbsolutePath().normalize();
        try {
            if (Files.size(normalizedPath) > largePreviewProperties.getFullRenderMaxBytes()) {
                return validateStreaming(normalizedPath);
            }
            String content = Files.readString(normalizedPath);
            if (content.isBlank()) {
                return new JsonValidationResult(JsonValidationStatus.EMPTY, EMPTY_FILE_MESSAGE, null, null);
            }

            objectMapper.readTree(content);
            return new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
        } catch (JsonProcessingException exception) {
            JsonLocation location = exception.getLocation();
            return new JsonValidationResult(
                    JsonValidationStatus.INVALID,
                    buildInvalidMessage(exception),
                    location == null ? null : (int) location.getLineNr(),
                    location == null ? null : (int) location.getColumnNr()
            );
        } catch (IOException exception) {
            return new JsonValidationResult(
                    JsonValidationStatus.PARSING_ERROR,
                    "Unable to read JSON file: " + exception.getMessage(),
                    null,
                    null
            );
        }
    }

    private JsonValidationResult validateStreaming(Path normalizedPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(normalizedPath);
             JsonParser parser = objectMapper.getFactory().createParser(inputStream)) {
            if (parser.nextToken() == null) {
                return new JsonValidationResult(JsonValidationStatus.EMPTY, EMPTY_FILE_MESSAGE, null, null);
            }

            objectMapper.readTree(parser);
            if (parser.nextToken() != null) {
                JsonLocation location = parser.currentLocation();
                return new JsonValidationResult(
                        JsonValidationStatus.INVALID,
                        "Invalid JSON: Unexpected trailing content after JSON document.",
                        location == null ? null : (int) location.getLineNr(),
                        location == null ? null : (int) location.getColumnNr()
                );
            }
            return new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON.", null, null);
        }
    }

    private String buildInvalidMessage(JsonProcessingException exception) {
        String originalMessage = exception.getOriginalMessage();
        if (originalMessage == null || originalMessage.isBlank()) {
            return "Invalid JSON.";
        }
        return "Invalid JSON: " + originalMessage;
    }
}
