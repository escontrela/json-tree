package com.davidpe.jsontree.ui.support;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Coordinates the delayed visibility and frame cadence of the large-preview page-loading
 * affordance shown inside the main viewer.
 *
 * <p>The affordance stays hidden for fast warm-page transitions and only reveals itself when a
 * paged large-preview request is still pending after a short delay. The owner remains responsible
 * for scheduling the reveal delay and frame cadence, while this class keeps the request token,
 * visibility and frame progression coherent across overlapping or canceled requests.
 */
public final class LargePreviewLoadingAffordance {

  private static final int FRAME_COUNT = 4;

  private final Runnable onReveal;
  private final Runnable onHide;
  private final IntConsumer onFrameChanged;

  private long requestSequence;
  private boolean visible;
  private int currentFrameIndex;

  public LargePreviewLoadingAffordance(
      Runnable onReveal, Runnable onHide, IntConsumer onFrameChanged) {
    this.onReveal = Objects.requireNonNull(onReveal, "Large-preview loader reveal callback is required.");
    this.onHide = Objects.requireNonNull(onHide, "Large-preview loader hide callback is required.");
    this.onFrameChanged =
        Objects.requireNonNull(
            onFrameChanged, "Large-preview loader frame-change callback is required.");
  }

  public long beginRequest() {
    completeRequest();
    return ++requestSequence;
  }

  public void completeRequest() {
    if (visible) {
      visible = false;
      onHide.run();
    }
    requestSequence++;
  }

  public boolean visible() {
    return visible;
  }

  public void revealIfPending(long sequence) {
    if (sequence != requestSequence) {
      return;
    }
    visible = true;
    currentFrameIndex = 0;
    onFrameChanged.accept(currentFrameIndex);
    onReveal.run();
  }

  public void advanceFrame(long sequence) {
    if (sequence != requestSequence || !visible) {
      return;
    }
    currentFrameIndex = (currentFrameIndex + 1) % FRAME_COUNT;
    onFrameChanged.accept(currentFrameIndex);
  }
}
