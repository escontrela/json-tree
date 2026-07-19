package com.davidpe.jsontree.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JavaFxUiThreadRunnerTest {

  @Test
  void runsImmediatelyWhenAlreadyOnFxThread() {
    JavaFxUiThreadRunner runner =
        new JavaFxUiThreadRunner(() -> true, runnable -> {}, 10L);

    String result = runner.call(() -> "clipboard-value");

    assertEquals("clipboard-value", result);
  }

  @Test
  void dispatchesWorkThroughSchedulerWhenCallerIsOffThread() {
    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    AtomicBoolean schedulerUsed = new AtomicBoolean(false);
    JavaFxUiThreadRunner runner =
        new JavaFxUiThreadRunner(
            () -> false,
            runnable -> {
              schedulerUsed.set(true);
              scheduled.set(runnable);
              runnable.run();
            },
            10L);

    String result = runner.call(() -> "clipboard-value");

    assertTrue(schedulerUsed.get());
    assertEquals("clipboard-value", result);
  }

  @Test
  void wrapsScheduledFailuresAsRuntimeErrors() {
    JavaFxUiThreadRunner runner =
        new JavaFxUiThreadRunner(
            () -> false,
            runnable -> runnable.run(),
            10L);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                runner.call(
                    () -> {
                      throw new IllegalStateException("clipboard boom");
                    }));

    assertEquals("clipboard boom", exception.getMessage());
  }
}
