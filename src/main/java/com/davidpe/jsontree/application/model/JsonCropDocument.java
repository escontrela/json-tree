package com.davidpe.jsontree.application.model;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;

/**
 * Ephemeral cropped JSON view derived from an existing source document and never persisted.
 */
public record JsonCropDocument(String rawJson, AsciiTreeDocument asciiTreeDocument) {}
