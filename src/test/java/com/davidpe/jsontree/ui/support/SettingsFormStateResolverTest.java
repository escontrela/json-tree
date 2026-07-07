package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.LargePreviewSettingsSnapshot;
import org.junit.jupiter.api.Test;

class SettingsFormStateResolverTest {

  private final SettingsFormStateResolver resolver = new SettingsFormStateResolver();

  @Test
  void rendersInitialValuesFromTheCurrentRuntimeSnapshot() {
    SettingsFormState state =
        resolver.initialState(new LargePreviewSettingsSnapshot(2_097_152L, 262_144), 8_388_608L);

    assertEquals("2097152", state.thresholdText());
    assertEquals("262144", state.chunkText());
    assertTrue(state.memoryReferenceText().contains("Startup JVM reference"));
    assertTrue(state.applyEnabled());
  }

  @Test
  void activatesWarningWhenThresholdExceedsStartupMemoryReference() {
    SettingsFormState state = resolver.resolve("10485760", "262144", 8_388_608L);

    assertTrue(state.warningActive());
    assertTrue(state.warningText().contains("exceeds"));
    assertTrue(state.applyEnabled());
  }

  @Test
  void disablesApplyAndShowsReadableErrorsForInvalidInput() {
    SettingsFormState state = resolver.resolve("abc", "", 8_388_608L);

    assertFalse(state.applyEnabled());
    assertTrue(state.thresholdErrorText().contains("whole number"));
    assertTrue(state.chunkErrorText().contains("Enter a chunk size"));
    assertFalse(state.warningActive());
  }
}
