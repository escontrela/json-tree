package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import java.util.Optional;

/**
 * Persists the editable large-preview settings outside the runtime/application layer.
 */
public interface LargePreviewSettingsStore {

  Optional<LargePreviewSettingsSnapshot> load();

  void save(LargePreviewSettingsSnapshot snapshot);
}
