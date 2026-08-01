package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ViewerTextScaleStepTest {

  @Test
  void cyclesBaseThenDoubleThenTripleAndBackToBase() {
    assertEquals(ViewerTextScaleStep.DOUBLE, ViewerTextScaleStep.BASE.next());
    assertEquals(ViewerTextScaleStep.TRIPLE, ViewerTextScaleStep.DOUBLE.next());
    assertEquals(ViewerTextScaleStep.BASE, ViewerTextScaleStep.TRIPLE.next());
  }
}
