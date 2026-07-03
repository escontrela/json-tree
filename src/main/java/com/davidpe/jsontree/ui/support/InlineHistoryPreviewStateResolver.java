package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InlineHistoryPreviewStateResolver {

  public InlineHistoryPreviewState resolve(
      List<ImportedJsonFile> allEntries, int maxVisibleEntries) {
    int safeMaxVisibleEntries = Math.max(0, maxVisibleEntries);
    List<ImportedJsonFile> visibleEntries =
        allEntries.size() <= safeMaxVisibleEntries
            ? List.copyOf(allEntries)
            : List.copyOf(
                allEntries.subList(
                    allEntries.size() - safeMaxVisibleEntries, allEntries.size()));

    return new InlineHistoryPreviewState(
        visibleEntries,
        visibleEntries.isEmpty()
            ? "No recent snapshots"
            : visibleEntries.size()
                + " recent snapshot"
                + (visibleEntries.size() == 1 ? "" : "s"));
  }
}
