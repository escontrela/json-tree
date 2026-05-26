package com.davidpe.jsontree.domain.model;

public record AsciiTreeDocument(
        String rootLabel,
        String content,
        int lineCount
) {
}
