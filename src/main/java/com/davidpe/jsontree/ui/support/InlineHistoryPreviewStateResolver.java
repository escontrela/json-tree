package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InlineHistoryPreviewStateResolver {

  public InlineHistoryPreviewState resolve(
      List<ImportedJsonFile> allEntries, int maxVisibleEntries) {
    int safeMaxVisibleEntries = Math.max(0, maxVisibleEntries);
    List<ImportedJsonFile> descendingEntries =
        allEntries.stream()
            .sorted(Comparator.comparing(ImportedJsonFile::importedAt).reversed())
            .toList();
    List<ImportedJsonFile> visibleEntries =
        descendingEntries.size() <= safeMaxVisibleEntries
            ? List.copyOf(descendingEntries)
            : List.copyOf(descendingEntries.subList(0, safeMaxVisibleEntries));

    return new InlineHistoryPreviewState(
        visibleEntries,
        visibleEntries.isEmpty()
            ? "No recent snapshots"
            : visibleEntries.size()
                + " recent snapshot"
                + (visibleEntries.size() == 1 ? "" : "s"));
  }
}
