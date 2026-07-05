package com.davidpe.jsontree.ui.support;

/**
 * One page-oriented anchor rendered inside the large-preview outline rail.
 */
public record LargePreviewOutlineStep(
    int pageIndex, String title, String meta, boolean active, double documentScrollValue) {}
