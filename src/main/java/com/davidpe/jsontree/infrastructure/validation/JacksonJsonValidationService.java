package com.davidpe.jsontree.infrastructure.validation;

import com.davidpe.jsontree.application.port.out.JsonValidationPort;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class JacksonJsonValidationService implements JsonValidationPort {

    private static final String EMPTY_FILE_MESSAGE = "File is empty.";

    private final ObjectMapper objectMapper;

    public JacksonJsonValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonValidationResult validate(Path jsonFilePath) {
        Path normalizedPath = jsonFilePath.toAbsolutePath().normalize();
        try {
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

    private String buildInvalidMessage(JsonProcessingException exception) {
        String originalMessage = exception.getOriginalMessage();
        if (originalMessage == null || originalMessage.isBlank()) {
            return "Invalid JSON.";
        }
        return "Invalid JSON: " + originalMessage;
    }
}
