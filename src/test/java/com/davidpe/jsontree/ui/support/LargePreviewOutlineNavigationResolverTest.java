package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonOutlineEntry;
import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;
import com.davidpe.jsontree.application.model.LargePreviewOutlineDigest;
import com.davidpe.jsontree.application.model.LargePreviewOutlineDigestEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

class LargePreviewOutlineNavigationResolverTest {

  private final LargePreviewOutlineNavigationResolver resolver =
      new LargePreviewOutlineNavigationResolver();

  @Test
  void resolvesForwardAndBackwardPageTargetsFromMinimapRows() {
    OutlineMinimapLayout layout =
        new OutlineMinimapLayout(
            List.of(
                new OutlineMinimapRow(10.0, 10.0, 40.0, 8.0, JsonOutlineEntryKind.OBJECT, 0, 2),
                new OutlineMinimapRow(10.0, 24.0, 40.0, 8.0, JsonOutlineEntryKind.ARRAY, 2, 4),
                new OutlineMinimapRow(10.0, 38.0, 40.0, 8.0, JsonOutlineEntryKind.VALUE, 4, 6)),
            6);
    LargePreviewOutlineDigest digest =
        new LargePreviewOutlineDigest(
            List.of(
                digestEntry(0, JsonOutlineEntryKind.OBJECT),
                digestEntry(0, JsonOutlineEntryKind.OBJECT),
                digestEntry(1, JsonOutlineEntryKind.ARRAY),
                digestEntry(1, JsonOutlineEntryKind.VALUE),
                digestEntry(3, JsonOutlineEntryKind.OBJECT),
                digestEntry(3, JsonOutlineEntryKind.VALUE)),
            2);

    assertEquals(0, resolver.targetPage(layout, digest, 12.0).getAsInt());
    assertEquals(1, resolver.targetPage(layout, digest, 26.0).getAsInt());
    assertEquals(3, resolver.targetPage(layout, digest, 42.0).getAsInt());
  }

  @Test
  void fallsBackToNearestRowWhenPointerMissesExactBar() {
    OutlineMinimapLayout layout =
        new OutlineMinimapLayout(
            List.of(new OutlineMinimapRow(10.0, 20.0, 30.0, 6.0, JsonOutlineEntryKind.OBJECT, 0, 1)),
            1);
    LargePreviewOutlineDigest digest =
        new LargePreviewOutlineDigest(List.of(digestEntry(4, JsonOutlineEntryKind.OBJECT)), 0);

    assertTrue(resolver.targetPage(layout, digest, 2.0).isPresent());
    assertEquals(4, resolver.targetPage(layout, digest, 2.0).getAsInt());
  }

  private LargePreviewOutlineDigestEntry digestEntry(int pageIndex, JsonOutlineEntryKind kind) {
    return new LargePreviewOutlineDigestEntry(pageIndex, new JsonOutlineEntry(0, 18, kind, 0));
  }
}
