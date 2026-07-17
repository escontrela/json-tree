package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ByteSizeFormatterTest {

  @Test
  void formatsBytesBelowOneKilobyte() {
    assertEquals("999 B", ByteSizeFormatter.format(999));
  }

  @Test
  void formatsExactKilobyteBoundary() {
    assertEquals("1.0 KB", ByteSizeFormatter.format(1024));
  }

  @Test
  void formatsExactMegabyteBoundary() {
    assertEquals("1.0 MB", ByteSizeFormatter.format(1024L * 1024L));
  }

  @Test
  void formatsExactGigabyteBoundary() {
    assertEquals("1.0 GB", ByteSizeFormatter.format(1024L * 1024L * 1024L));
  }
}
