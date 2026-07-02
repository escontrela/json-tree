package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonOutlineEntry;
import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OutlineMinimapLayoutPlanner {

  private static final double HORIZONTAL_INSET = 12.0;
  private static final double VERTICAL_INSET = 14.0;
  private static final double MIN_ROW_HEIGHT = 3.0;
  private static final double MAX_ROW_HEIGHT = 5.0;
  private static final double ROW_GAP = 2.0;
  private static final double MIN_BAR_WIDTH = 18.0;

  public OutlineMinimapLayout plan(JsonOutlineModel model, double width, double height) {
    if (model == null
        || model.emptyModel()
        || width <= (HORIZONTAL_INSET * 2.0)
        || height <= (VERTICAL_INSET * 2.0)) {
      return OutlineMinimapLayout.empty();
    }

    double drawableWidth = width - (HORIZONTAL_INSET * 2.0);
    double drawableHeight = height - (VERTICAL_INSET * 2.0);
    if (drawableWidth <= MIN_BAR_WIDTH || drawableHeight < MIN_ROW_HEIGHT) {
      return OutlineMinimapLayout.empty();
    }

    int visibleRows = resolveVisibleRows(model.totalEntries(), drawableHeight);
    double rowHeight = resolveRowHeight(visibleRows, drawableHeight);
    double depthStep = resolveDepthStep(model.maxDepth(), drawableWidth);
    double entriesPerRow = (double) model.totalEntries() / visibleRows;

    List<OutlineMinimapRow> rows = new ArrayList<>(visibleRows);
    for (int rowIndex = 0; rowIndex < visibleRows; rowIndex++) {
      int sourceStart = (int) Math.floor(rowIndex * entriesPerRow);
      int sourceEnd = Math.min(model.totalEntries(), (int) Math.ceil((rowIndex + 1) * entriesPerRow));
      if (sourceStart >= sourceEnd) {
        sourceEnd = Math.min(model.totalEntries(), sourceStart + 1);
      }

      List<JsonOutlineEntry> bucket = model.entries().subList(sourceStart, sourceEnd);
      int depth = aggregatedDepth(bucket);
      int visualWeight = aggregatedVisualWeight(bucket);
      JsonOutlineEntryKind kind = dominantKind(bucket);

      double indentOffset = Math.min(drawableWidth - MIN_BAR_WIDTH, depth * depthStep);
      double x = HORIZONTAL_INSET + indentOffset;
      double maxBarWidth = Math.max(MIN_BAR_WIDTH, width - x - HORIZONTAL_INSET);
      double barWidth = clamp((drawableWidth * visualWeight) / 30.0, MIN_BAR_WIDTH, maxBarWidth);
      double y = VERTICAL_INSET + rowIndex * (rowHeight + ROW_GAP);
      rows.add(new OutlineMinimapRow(x, y, barWidth, rowHeight, kind, sourceStart, sourceEnd));
    }

    return new OutlineMinimapLayout(rows, model.totalEntries());
  }

  private int resolveVisibleRows(int totalEntries, double drawableHeight) {
    int capacity = (int) Math.floor((drawableHeight + ROW_GAP) / (MIN_ROW_HEIGHT + ROW_GAP));
    return Math.max(1, Math.min(totalEntries, capacity));
  }

  private double resolveRowHeight(int visibleRows, double drawableHeight) {
    double heightBudget = drawableHeight - ((visibleRows - 1) * ROW_GAP);
    return clamp(heightBudget / visibleRows, MIN_ROW_HEIGHT, MAX_ROW_HEIGHT);
  }

  private double resolveDepthStep(int maxDepth, double drawableWidth) {
    if (maxDepth <= 0) {
      return 0.0;
    }
    return clamp(drawableWidth / (maxDepth + 3.0), 4.0, 10.0);
  }

  private int aggregatedDepth(List<JsonOutlineEntry> bucket) {
    return (int) Math.round(bucket.stream().mapToInt(JsonOutlineEntry::depth).average().orElse(0.0));
  }

  private int aggregatedVisualWeight(List<JsonOutlineEntry> bucket) {
    return bucket.stream().mapToInt(JsonOutlineEntry::visualWeight).max().orElse(6);
  }

  private JsonOutlineEntryKind dominantKind(List<JsonOutlineEntry> bucket) {
    boolean hasObject = bucket.stream().anyMatch(entry -> entry.kind() == JsonOutlineEntryKind.OBJECT);
    if (hasObject) {
      return JsonOutlineEntryKind.OBJECT;
    }
    boolean hasArray = bucket.stream().anyMatch(entry -> entry.kind() == JsonOutlineEntryKind.ARRAY);
    if (hasArray) {
      return JsonOutlineEntryKind.ARRAY;
    }
    return JsonOutlineEntryKind.VALUE;
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
