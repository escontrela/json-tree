package com.davidpe.jsontree.ui.service;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.springframework.stereotype.Service;

/**
 * Reusable UI effect service that reveals label text progressively.
 *
 * <p>The service is presentation-layer only. It owns interruption and replacement semantics so
 * controllers can simply request the next target text for a label without manually coordinating
 * JavaFX timelines.
 */
@Service
public class TypewriterLabelRevealService {

  private final Map<Object, ActiveReveal> activeReveals = new WeakHashMap<>();
  private final TypewriterScheduler scheduler;

  public TypewriterLabelRevealService() {
    this(new JavaFxTypewriterScheduler());
  }

  TypewriterLabelRevealService(TypewriterScheduler scheduler) {
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
  }

  public CompletableFuture<Void> reveal(Label label, String targetText) {
    return reveal(label, targetText, TypewriterRevealConfig.subtleFast());
  }

  public CompletableFuture<Void> reveal(
      Label label, String targetText, TypewriterRevealConfig config) {
    Objects.requireNonNull(label, "label must not be null");
    Objects.requireNonNull(config, "config must not be null");

    if (Platform.isFxApplicationThread()) {
      return revealInternal(label, new LabelTextTarget(label), targetText, config);
    }

    CompletableFuture<Void> bridgedCompletion = new CompletableFuture<>();
    Platform.runLater(
        () ->
            revealInternal(label, new LabelTextTarget(label), targetText, config)
                .whenComplete(
                    (unused, throwable) -> {
                      if (throwable != null) {
                        bridgedCompletion.completeExceptionally(throwable);
                        return;
                      }
                      bridgedCompletion.complete(null);
                    }));
    return bridgedCompletion;
  }

  CompletableFuture<Void> reveal(
      TypewriterTextTarget target, String targetText, TypewriterRevealConfig config) {
    Objects.requireNonNull(target, "target must not be null");
    Objects.requireNonNull(config, "config must not be null");
    return revealInternal(target, target, targetText, config);
  }

  private CompletableFuture<Void> revealInternal(
      Object revealKey,
      TypewriterTextTarget target,
      String targetText,
      TypewriterRevealConfig config) {
    String normalizedText = targetText == null ? "" : targetText;
    ActiveReveal existingReveal = activeReveals.get(revealKey);
    if (existingReveal != null) {
      if (existingReveal.matches(normalizedText)) {
        return existingReveal.completion();
      }
      cancelReveal(revealKey, existingReveal);
    } else if (Objects.equals(target.text(), normalizedText)) {
      return CompletableFuture.completedFuture(null);
    }

    target.updateText("");
    if (normalizedText.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> completion = new CompletableFuture<>();
    int[] visibleCharacters = new int[] {0};
    ScheduledReveal[] scheduledRevealReference = new ScheduledReveal[1];
    ScheduledReveal scheduledReveal =
        scheduler.schedule(
            config,
            () -> {
              visibleCharacters[0] = Math.min(normalizedText.length(), visibleCharacters[0] + 1);
              target.updateText(normalizedText.substring(0, visibleCharacters[0]));
              if (visibleCharacters[0] < normalizedText.length()) {
                return;
              }
              scheduledRevealReference[0].stop();
              ActiveReveal activeReveal = activeReveals.get(revealKey);
              if (activeReveal != null
                  && activeReveal.scheduledReveal() == scheduledRevealReference[0]) {
                activeReveals.remove(revealKey);
              }
              completion.complete(null);
            });
    scheduledRevealReference[0] = scheduledReveal;

    ActiveReveal nextReveal = new ActiveReveal(normalizedText, scheduledReveal, completion);
    activeReveals.put(revealKey, nextReveal);
    scheduledReveal.start();
    return completion;
  }

  private void cancelReveal(Object revealKey, ActiveReveal activeReveal) {
    activeReveal.scheduledReveal().stop();
    activeReveals.remove(revealKey);
    activeReveal
        .completion()
        .completeExceptionally(new CancellationException("Label reveal superseded"));
  }

  private record ActiveReveal(
      String targetText,
      ScheduledReveal scheduledReveal,
      CompletableFuture<Void> completion) {

    private boolean matches(String nextTargetText) {
      return targetText.equals(nextTargetText) && !completion.isDone();
    }
  }

  interface TypewriterTextTarget {
    String text();

    void updateText(String text);
  }

  interface TypewriterScheduler {
    ScheduledReveal schedule(TypewriterRevealConfig config, Runnable tick);
  }

  interface ScheduledReveal {
    void start();

    void stop();
  }

  private record LabelTextTarget(Label label) implements TypewriterTextTarget {

    @Override
    public String text() {
      return label.getText();
    }

    @Override
    public void updateText(String text) {
      label.setText(text);
    }
  }

  private static final class JavaFxTypewriterScheduler implements TypewriterScheduler {

    @Override
    public ScheduledReveal schedule(TypewriterRevealConfig config, Runnable tick) {
      Timeline timeline =
          new Timeline(
              new KeyFrame(
                  javafx.util.Duration.millis(config.characterDelay().toMillis()),
                  event -> tick.run()));
      timeline.setDelay(javafx.util.Duration.millis(config.initialDelay().toMillis()));
      timeline.setCycleCount(Timeline.INDEFINITE);
      return new ScheduledReveal() {
        @Override
        public void start() {
          timeline.playFromStart();
        }

        @Override
        public void stop() {
          timeline.stop();
        }
      };
    }
  }
}
