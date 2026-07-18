package com.davidpe.jsontree.ui.service;

import java.time.Duration;
import java.util.Objects;

/**
 * Presentation-only configuration for a typewriter-style label reveal.
 *
 * <p>The defaults are intentionally subtle and fast so short status labels feel animated without
 * slowing the workflow.
 */
public record TypewriterRevealConfig(Duration initialDelay, Duration characterDelay) {

  private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(18);
  private static final Duration DEFAULT_CHARACTER_DELAY = Duration.ofMillis(10);

  public TypewriterRevealConfig {
    Objects.requireNonNull(initialDelay, "initialDelay must not be null");
    Objects.requireNonNull(characterDelay, "characterDelay must not be null");
    if (initialDelay.isNegative()) {
      throw new IllegalArgumentException("initialDelay must not be negative");
    }
    if (characterDelay.isNegative() || characterDelay.isZero()) {
      throw new IllegalArgumentException("characterDelay must be greater than zero");
    }
  }

  public static TypewriterRevealConfig subtleFast() {
    return new TypewriterRevealConfig(DEFAULT_INITIAL_DELAY, DEFAULT_CHARACTER_DELAY);
  }
}
