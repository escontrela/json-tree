package com.davidpe.jsontree.infrastructure.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.application.Platform;
import org.springframework.stereotype.Component;

/**
 * Executes small UI-bound tasks on the JavaFX application thread, even when callers run from a
 * background workflow thread.
 */
@Component
public class JavaFxUiThreadRunner {

  private static final long DEFAULT_TIMEOUT_MILLIS = 5_000L;

  private final BooleanSupplier fxThreadDetector;
  private final Consumer<Runnable> scheduler;
  private final long timeoutMillis;

  public JavaFxUiThreadRunner() {
    this(Platform::isFxApplicationThread, Platform::runLater, DEFAULT_TIMEOUT_MILLIS);
  }

  JavaFxUiThreadRunner(
      BooleanSupplier fxThreadDetector, Consumer<Runnable> scheduler, long timeoutMillis) {
    this.fxThreadDetector = fxThreadDetector;
    this.scheduler = scheduler;
    this.timeoutMillis = timeoutMillis;
  }

  /**
   * Runs a callable on the JavaFX application thread and returns its value to the caller.
   */
  public <T> T call(Callable<T> callable) {
    if (fxThreadDetector.getAsBoolean()) {
      return invoke(callable);
    }

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<RuntimeException> failure = new AtomicReference<>();
    scheduler.accept(
        () -> {
          try {
            result.set(invoke(callable));
          } catch (RuntimeException exception) {
            failure.set(exception);
          } finally {
            latch.countDown();
          }
        });

    try {
      if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
        throw new IllegalStateException("Timed out waiting for JavaFX UI-thread clipboard access.");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for JavaFX UI-thread access.", exception);
    }

    if (failure.get() != null) {
      throw failure.get();
    }
    return result.get();
  }

  /**
   * Runs a fire-and-forget task on the JavaFX application thread and waits until it completes.
   */
  public void run(Runnable runnable) {
    call(
        () -> {
          runnable.run();
          return null;
        });
  }

  private <T> T invoke(Callable<T> callable) {
    try {
      return callable.call();
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("JavaFX UI-thread task failed.", exception);
    }
  }
}
