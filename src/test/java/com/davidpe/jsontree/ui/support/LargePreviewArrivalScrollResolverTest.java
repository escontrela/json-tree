package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LargePreviewArrivalScrollResolverTest {

  private final LargePreviewArrivalScrollResolver resolver = new LargePreviewArrivalScrollResolver();

  @Test
  void landsNearTopWhenAdvancingIntoNextChunk() {
    double arrival =
        resolver.resolve(
            new LargePreviewPageDescriptor(
                1, Path.of("/tmp/source.json"), 1024L, 153_600, 12_288, 12_288),
            1);

    assertTrue(arrival > 0.0);
    assertTrue(arrival < 0.4);
  }

  @Test
  void landsNearBottomWhenReturningToPreviousChunk() {
    double arrival =
        resolver.resolve(
            new LargePreviewPageDescriptor(
                1, Path.of("/tmp/source.json"), 1024L, 153_600, 12_288, 12_288),
            -1);

    assertTrue(arrival > 0.6);
    assertTrue(arrival < 1.0);
  }

  @Test
  void keepsDefaultEdgesWhenThereIsNoDirectionalJump() {
    LargePreviewPageDescriptor descriptor =
        new LargePreviewPageDescriptor(0, Path.of("/tmp/source.json"), 0L, 0, 0, 0);

    assertEquals(0.0, resolver.resolve(descriptor, 1));
    assertEquals(1.0, resolver.resolve(descriptor, -1));
  }
}
