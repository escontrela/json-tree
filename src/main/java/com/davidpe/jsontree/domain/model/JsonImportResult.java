package com.davidpe.jsontree.domain.model;

import java.nio.file.Path;

public record JsonImportResult(
        Path path,
        String fileName,
        long sizeBytes,
        boolean exists,
        boolean readable,
        boolean regularFile
) {

    public boolean available() {
        return exists && readable && regularFile;
    }
}
