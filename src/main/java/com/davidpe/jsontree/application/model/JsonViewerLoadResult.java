package com.davidpe.jsontree.application.model;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;

public record JsonViewerLoadResult(
        JsonImportResult importResult,
        JsonValidationResult validationResult,
        AsciiTreeDocument asciiTreeDocument,
        ImportedJsonFile historyEntry
) {

    public boolean hasRenderableTree() {
        return asciiTreeDocument != null && validationResult.valid();
    }
}
