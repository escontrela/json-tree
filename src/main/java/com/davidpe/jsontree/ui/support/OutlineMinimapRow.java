package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;

public record OutlineMinimapRow(
    double x,
    double y,
    double width,
    double height,
    JsonOutlineEntryKind kind,
    int sourceIndexStart,
    int sourceIndexEnd
) {

  public OutlineMinimapRow {
    if (width < 0.0 || height < 0.0) {
      throw new IllegalArgumentException("Outline minimap rows must use non-negative geometry.");
    }
    if (sourceIndexStart < 0 || sourceIndexEnd < sourceIndexStart) {
      throw new IllegalArgumentException("Outline minimap source indexes are invalid.");
    }
  }
}
