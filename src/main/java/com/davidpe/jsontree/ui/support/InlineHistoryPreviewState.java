package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;

public record InlineHistoryPreviewState(
    List<ImportedJsonFile> visibleEntries, String summaryLabel) {}
