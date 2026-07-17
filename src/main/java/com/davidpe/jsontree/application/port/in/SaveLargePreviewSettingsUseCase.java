package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;

/**
 * Input port for persisting and activating a new editable large-preview settings snapshot.
 */
public interface SaveLargePreviewSettingsUseCase {

  LargePreviewSettingsSnapshot saveLargePreviewSettings(LargePreviewSettingsSnapshot snapshot);
}
