package com.davidpe.jsontree.ui.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;

final class JavaFxThreadTestSupport {

  private static final AtomicBoolean STARTED = new AtomicBoolean(false);

  private JavaFxThreadTestSupport() {}

  static void startPlatform() {
    if (STARTED.compareAndSet(false, true)) {
      configureHeadlessJavaFx();
      CountDownLatch latch = new CountDownLatch(1);
      Platform.startup(latch::countDown);
      await(latch);
    }
  }

  static void runOnFxThread(Runnable action) {
    startPlatform();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Platform.runLater(
        () -> {
          try {
            action.run();
          } catch (Throwable throwable) {
            failure.set(throwable);
          } finally {
            latch.countDown();
          }
        });
    await(latch);
    if (failure.get() != null) {
      throw new AssertionError(failure.get());
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for JavaFX");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
  }

  private static void configureHeadlessJavaFx() {
    System.setProperty("prism.order", "sw");
    System.setProperty("java.awt.headless", "true");
    if (System.getProperty("javafx.cachedir") == null) {
      try {
        Path cacheDir = Files.createTempDirectory("openjfx-cache-");
        System.setProperty("javafx.cachedir", cacheDir.toAbsolutePath().toString());
      } catch (IOException exception) {
        throw new AssertionError(exception);
      }
    }
  }
}
