package com.davidpe.jsontree.application.model;

public record RawJsonPresentation(
    String content,
    int[] sourceToDisplayBoundaries
) {}
