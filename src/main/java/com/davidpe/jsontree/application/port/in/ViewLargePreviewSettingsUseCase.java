package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;

/**
 * Input port for reading the current editable large-preview settings snapshot.
 */
public interface ViewLargePreviewSettingsUseCase {

  LargePreviewSettingsSnapshot currentLargePreviewSettings();
}
