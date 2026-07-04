package com.davidpe.jsontree.domain.model;

public record JsonValidationResult(
        JsonValidationStatus status,
        String message,
        Integer line,
        Integer column
) {

    public boolean valid() {
        return status == JsonValidationStatus.VALID;
    }
}
