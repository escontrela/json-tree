package com.davidpe.jsontree.domain.model;

public record JsonValidationResult(
        boolean valid,
        String message,
        Integer line,
        Integer column
) {
}
