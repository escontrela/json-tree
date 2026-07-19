package com.davidpe.jsontree.application.model;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;

public record JsonViewerLoadResult(
        JsonImportResult importResult,
        JsonValidationResult validationResult,
        AsciiTreeDocument asciiTreeDocument,
        ImportedJsonFile historyEntry,
        JsonInspectionMode inspectionMode,
        JsonViewerCapabilities capabilities,
        LargePreviewPagedSession largePreviewSession
) {

    public boolean hasRenderableTree() {
        return asciiTreeDocument != null
                && validationResult.valid()
                && importResult.documentFormat().json();
    }

    public boolean usesLargePreview() {
        return inspectionMode == JsonInspectionMode.LARGE_PREVIEW;
    }

    public boolean hasLargePreviewSession() {
        return largePreviewSession != null;
    }

    public boolean markdownDocument() {
        return importResult.documentFormat().markdown();
    }

    public JsonViewerLoadResult withLargePreviewSession(LargePreviewPagedSession nextLargePreviewSession) {
        return new JsonViewerLoadResult(
                importResult,
                validationResult,
                asciiTreeDocument,
                historyEntry,
                inspectionMode,
                capabilities,
                nextLargePreviewSession
        );
    }
}
