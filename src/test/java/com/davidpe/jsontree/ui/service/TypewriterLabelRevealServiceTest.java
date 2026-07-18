package com.davidpe.jsontree.ui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class TypewriterLabelRevealServiceTest {

  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(2);

  @Test
  void completesRevealWithTheExactTargetText() throws Exception {
    ManualTypewriterScheduler scheduler = new ManualTypewriterScheduler();
    TypewriterLabelRevealService service = new TypewriterLabelRevealService(scheduler);
    RecordingTextTarget label = new RecordingTextTarget();

    CompletableFuture<Void> completion =
        service.reveal(label, "History snapshot unavailable", fastConfig());

    scheduler.runToCompletion();
    completion.get(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

    assertEquals("History snapshot unavailable", label.text());
  }

  @Test
  void cancelsThePreviousRevealWhenANewerTextArrives() throws Exception {
    ManualTypewriterScheduler scheduler = new ManualTypewriterScheduler();
    TypewriterLabelRevealService service = new TypewriterLabelRevealService(scheduler);
    RecordingTextTarget label = new RecordingTextTarget();

    CompletableFuture<Void> firstReveal =
        service.reveal(label, "Loading JSON preview", slowConfig());
    scheduler.advanceOneTick();

    waitUntil(
        () -> {
          String currentText = label.text();
          return !currentText.isEmpty() && currentText.length() < "Loading JSON preview".length();
        });

    CompletableFuture<Void> secondReveal =
        service.reveal(label, "JSON ready", fastConfig());
    scheduler.runToCompletion();

    try {
      firstReveal.get(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      fail("The superseded reveal should have been cancelled");
    } catch (java.util.concurrent.CancellationException exception) {
      assertTrue(exception.getMessage().contains("superseded"));
    }

    secondReveal.get(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    assertEquals("JSON ready", label.text());
  }

  @Test
  void reusesTheSameServiceAcrossMultipleLabelsIndependently() throws Exception {
    ManualTypewriterScheduler scheduler = new ManualTypewriterScheduler();
    TypewriterLabelRevealService service = new TypewriterLabelRevealService(scheduler);
    RecordingTextTarget fileNameLabel = new RecordingTextTarget();
    RecordingTextTarget footerStatusLabel = new RecordingTextTarget();

    CompletableFuture<Void> fileNameReveal =
        service.reveal(fileNameLabel, "sample.json", fastConfig());
    CompletableFuture<Void> footerReveal =
        service.reveal(footerStatusLabel, "Reopened from history", fastConfig());

    scheduler.runToCompletion();
    fileNameReveal.get(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    footerReveal.get(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

    assertEquals("sample.json", fileNameLabel.text());
    assertEquals("Reopened from history", footerStatusLabel.text());
  }

  private static TypewriterRevealConfig fastConfig() {
    return new TypewriterRevealConfig(Duration.ZERO, Duration.ofMillis(4));
  }

  private static TypewriterRevealConfig slowConfig() {
    return new TypewriterRevealConfig(Duration.ZERO, Duration.ofMillis(20));
  }

  private static void waitUntil(BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + TEST_TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(10L);
    }
    fail("Timed out waiting for the typewriter reveal condition");
  }

  private static final class RecordingTextTarget
      implements TypewriterLabelRevealService.TypewriterTextTarget {

    private String text = "";

    @Override
    public String text() {
      return text;
    }

    @Override
    public void updateText(String text) {
      this.text = text;
    }
  }

  private static final class ManualTypewriterScheduler
      implements TypewriterLabelRevealService.TypewriterScheduler {

    private final List<ManualScheduledReveal> reveals = new ArrayList<>();

    @Override
    public TypewriterLabelRevealService.ScheduledReveal schedule(
        TypewriterRevealConfig config, Runnable tick) {
      ManualScheduledReveal reveal = new ManualScheduledReveal(tick);
      reveals.add(reveal);
      return reveal;
    }

    private void advanceOneTick() {
      reveals.stream()
          .filter(ManualScheduledReveal::started)
          .filter(reveal -> !reveal.stopped())
          .forEach(ManualScheduledReveal::tickOnce);
    }

    private void runToCompletion() {
      boolean active;
      do {
        active = false;
        for (ManualScheduledReveal reveal : reveals) {
          if (!reveal.started() || reveal.stopped()) {
            continue;
          }
          active = true;
          reveal.tickOnce();
        }
      } while (active);
    }
  }

  private static final class ManualScheduledReveal
      implements TypewriterLabelRevealService.ScheduledReveal {

    private final Runnable tick;
    private boolean started;
    private boolean stopped;

    private ManualScheduledReveal(Runnable tick) {
      this.tick = tick;
    }

    @Override
    public void start() {
      started = true;
    }

    @Override
    public void stop() {
      stopped = true;
    }

    private boolean started() {
      return started;
    }

    private boolean stopped() {
      return stopped;
    }

    private void tickOnce() {
      if (!started || stopped) {
        return;
      }
      tick.run();
    }
  }
}
