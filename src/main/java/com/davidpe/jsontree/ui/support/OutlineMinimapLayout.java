package com.davidpe.jsontree.ui.support;

import java.util.List;

public record OutlineMinimapLayout(
    List<OutlineMinimapRow> rows,
    int totalEntries
) {

  public OutlineMinimapLayout {
    rows = List.copyOf(rows);
    if (totalEntries < 0) {
      throw new IllegalArgumentException("Outline minimap total entries must be zero or greater.");
    }
  }

  public static OutlineMinimapLayout empty() {
    return new OutlineMinimapLayout(List.of(), 0);
  }

  public boolean emptyLayout() {
    return rows.isEmpty();
  }
}
