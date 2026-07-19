package com.davidpe.jsontree.ui.support;

public record ViewerCapabilityPresentation(
    boolean rawJsonEnabled,
    boolean structureEnabled,
    boolean searchEnabled,
    boolean outlineEnabled,
    String copyButtonText,
    String validationBadgeText,
    String validationBadgeStyleClass,
    String fileMetaSuffix,
    String footerStatus,
    String statusState,
    String outlineTitle,
    String outlineStateMessage,
    String outlineMetaMessage) {}
