package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LargePreviewLoadingAffordanceTest {

  @Test
  void revealsAndAdvancesFramesOnlyWhileTheCurrentRequestIsStillPending() {
    AtomicInteger revealCount = new AtomicInteger();
    AtomicInteger hideCount = new AtomicInteger();
    List<Integer> frames = new ArrayList<>();
    LargePreviewLoadingAffordance affordance =
        new LargePreviewLoadingAffordance(
            revealCount::incrementAndGet, hideCount::incrementAndGet, frames::add);

    long requestSequence = affordance.beginRequest();
    affordance.revealIfPending(requestSequence);
    affordance.advanceFrame(requestSequence);
    affordance.advanceFrame(requestSequence);

    assertTrue(affordance.visible());
    assertEquals(1, revealCount.get());
    assertEquals(List.of(0, 1, 2), frames);

    affordance.completeRequest();

    assertFalse(affordance.visible());
    assertEquals(1, hideCount.get());
  }

  @Test
  void ignoresDelayedRevealFromAnAlreadyCompletedWarmPageRequest() {
    AtomicInteger revealCount = new AtomicInteger();
    AtomicInteger hideCount = new AtomicInteger();
    LargePreviewLoadingAffordance affordance =
        new LargePreviewLoadingAffordance(
            revealCount::incrementAndGet, hideCount::incrementAndGet, unused -> {});

    long requestSequence = affordance.beginRequest();
    affordance.completeRequest();
    affordance.revealIfPending(requestSequence);
    affordance.advanceFrame(requestSequence);

    assertFalse(affordance.visible());
    assertEquals(0, revealCount.get());
    assertEquals(0, hideCount.get());
  }

  @Test
  void ignoresStaleRevealWhenANewerColdRequestHasAlreadyStarted() {
    AtomicInteger revealCount = new AtomicInteger();
    AtomicInteger hideCount = new AtomicInteger();
    LargePreviewLoadingAffordance affordance =
        new LargePreviewLoadingAffordance(
            revealCount::incrementAndGet, hideCount::incrementAndGet, unused -> {});

    long firstRequest = affordance.beginRequest();
    long secondRequest = affordance.beginRequest();
    affordance.revealIfPending(firstRequest);
    affordance.revealIfPending(secondRequest);

    assertTrue(affordance.visible());
    assertEquals(1, revealCount.get());
    assertEquals(0, hideCount.get());
  }
}
