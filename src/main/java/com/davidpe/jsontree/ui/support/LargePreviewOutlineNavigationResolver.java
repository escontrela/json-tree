package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.LargePreviewOutlineDigest;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalInt;
import org.springframework.stereotype.Component;

/**
 * Resolves a pointer interaction inside the existing minimap shell into a target page for a
 * paged large-preview session.
 */
@Component
public class LargePreviewOutlineNavigationResolver {

  public OptionalInt targetPage(
      OutlineMinimapLayout layout, LargePreviewOutlineDigest digest, double pointerY) {
    if (layout == null || layout.emptyLayout() || digest == null || digest.emptyDigest()) {
      return OptionalInt.empty();
    }

    Optional<OutlineMinimapRow> containingRow =
        layout.rows().stream()
            .filter(row -> pointerY >= row.y() && pointerY <= row.y() + row.height())
            .findFirst();
    OutlineMinimapRow targetRow =
        containingRow.orElseGet(
            () ->
                layout.rows().stream()
                    .min(
                        Comparator.comparingDouble(
                            row -> Math.abs((row.y() + (row.height() / 2.0)) - pointerY)))
                    .orElse(layout.rows().getFirst()));

    int entryIndex = Math.max(targetRow.sourceIndexStart(), targetRow.sourceIndexEnd() - 1);
    return digest.pageIndexForEntry(
        Math.min(entryIndex, Math.max(0, digest.entries().size() - 1)));
  }
}
