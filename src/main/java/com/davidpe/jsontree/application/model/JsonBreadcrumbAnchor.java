package com.davidpe.jsontree.application.model;

public record JsonBreadcrumbAnchor(
    JsonBreadcrumbPath path, int asciiLineIndex, int rawDisplayLineIndex) {}
