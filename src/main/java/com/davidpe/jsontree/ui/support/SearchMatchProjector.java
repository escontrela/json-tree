package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonSearchMatch;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SearchMatchProjector {

  public List<SearchHighlightRange> rawRanges(JsonSearchSession session) {
    return rawRanges(session, null);
  }

  public List<SearchHighlightRange> rawRanges(
      JsonSearchSession session,
      int[] sourceToDisplayBoundaries
  ) {
    List<SearchHighlightRange> ranges = new ArrayList<>();
    for (int index = 0; index < session.matches().size(); index++) {
      JsonSearchMatch match = session.matches().get(index);
      int projectedStart = projectBoundary(match.startIndex(), sourceToDisplayBoundaries);
      int projectedEnd = projectBoundary(match.endIndex(), sourceToDisplayBoundaries);
      ranges.add(new SearchHighlightRange(projectedStart, projectedEnd, index == session.activeMatchIndex()));
    }
    return ranges;
  }

  public List<SearchHighlightRange> asciiRanges(String asciiContent, JsonSearchSession session) {
    List<SearchHighlightRange> ranges = new ArrayList<>();
    int searchFrom = 0;
    for (int index = 0; index < session.matches().size(); index++) {
      String fragment = session.matches().get(index).fragment();
      if (fragment == null || fragment.isEmpty()) {
        continue;
      }

      int projectedStart = asciiContent.indexOf(fragment, searchFrom);
      if (projectedStart < 0) {
        continue;
      }

      int projectedEnd = projectedStart + fragment.length();
      ranges.add(new SearchHighlightRange(projectedStart, projectedEnd, index == session.activeMatchIndex()));
      searchFrom = projectedEnd;
    }
    return ranges;
  }

  private int projectBoundary(int sourceIndex, int[] sourceToDisplayBoundaries) {
    if (sourceToDisplayBoundaries == null || sourceIndex < 0 || sourceIndex >= sourceToDisplayBoundaries.length) {
      return sourceIndex;
    }
    return sourceToDisplayBoundaries[sourceIndex];
  }
}
