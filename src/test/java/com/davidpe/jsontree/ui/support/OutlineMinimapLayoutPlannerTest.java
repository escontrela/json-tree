package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonOutlineEntry;
import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutlineMinimapLayoutPlannerTest {

  private final OutlineMinimapLayoutPlanner planner = new OutlineMinimapLayoutPlanner();

  @Test
  void plansVisibleRowsWithinCanvasBounds() {
    JsonOutlineModel model =
        new JsonOutlineModel(
            List.of(
                new JsonOutlineEntry(0, 18, JsonOutlineEntryKind.OBJECT, 4),
                new JsonOutlineEntry(1, 14, JsonOutlineEntryKind.OBJECT, 2),
                new JsonOutlineEntry(2, 10, JsonOutlineEntryKind.VALUE, 0),
                new JsonOutlineEntry(1, 16, JsonOutlineEntryKind.ARRAY, 3),
                new JsonOutlineEntry(2, 10, JsonOutlineEntryKind.VALUE, 0)),
            2);

    OutlineMinimapLayout layout = planner.plan(model, 140.0, 220.0);

    assertFalse(layout.emptyLayout());
    assertEquals(model.totalEntries(), layout.totalEntries());
    assertTrue(layout.rows().stream().allMatch(row -> row.x() >= 12.0));
    assertTrue(layout.rows().stream().allMatch(row -> row.width() >= 18.0));
    assertTrue(layout.rows().stream().allMatch(row -> row.y() >= 14.0));
  }

  @Test
  void samplesLargeModelsToAvailableVerticalCapacity() {
    List<JsonOutlineEntry> entries =
        java.util.stream.IntStream.range(0, 180)
            .mapToObj(
                index ->
                    new JsonOutlineEntry(
                        index % 6,
                        10 + (index % 8),
                        index % 5 == 0 ? JsonOutlineEntryKind.OBJECT : JsonOutlineEntryKind.VALUE,
                        0))
            .toList();

    OutlineMinimapLayout layout = planner.plan(new JsonOutlineModel(entries, 5), 120.0, 180.0);

    assertFalse(layout.emptyLayout());
    assertTrue(layout.rows().size() < entries.size());
    assertEquals(0, layout.rows().getFirst().sourceIndexStart());
    assertEquals(entries.size(), layout.rows().getLast().sourceIndexEnd());
  }
}
