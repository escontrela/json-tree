package com.davidpe.jsontree.domain.model;

import java.time.Instant;

public record ImportedJsonFile(
        String storedName,
        String originalName,
        Instant importedAt,
        long sizeBytes,
        int lineCount,
        boolean valid,
        boolean favorite
) {

    public ImportedJsonFile withFavorite(boolean nextFavorite) {
        return new ImportedJsonFile(
                storedName,
                originalName,
                importedAt,
                sizeBytes,
                lineCount,
                valid,
                nextFavorite
        );
    }
}
