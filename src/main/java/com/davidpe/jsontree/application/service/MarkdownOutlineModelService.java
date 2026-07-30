package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonOutlineEntry;
import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.application.model.MarkdownOutlineEntry;
import com.davidpe.jsontree.application.model.MarkdownOutlineModel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Builds a compact Markdown outline using ATX headings first and a deterministic fallback when no
 * headings exist.
 */
@Service
public class MarkdownOutlineModelService {

  private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
  private static final Pattern FENCE_PATTERN = Pattern.compile("^```.*$");
  private static final int FALLBACK_SEGMENT_SIZE = 12;
  private static final int FILLER_VISUAL_WEIGHT = 4;

  public MarkdownOutlineModel build(String content) {
    if (content == null || content.isBlank()) {
      return MarkdownOutlineModel.empty();
    }

    String[] lines = content.split("\\R", -1);
    List<MarkdownOutlineEntry> headingEntries = buildHeadingEntries(lines);
    if (!headingEntries.isEmpty()) {
      int maxDepth = headingEntries.stream().mapToInt(MarkdownOutlineEntry::depth).max().orElse(0);
      return new MarkdownOutlineModel(headingEntries, maxDepth, true, lines.length);
    }

    List<MarkdownOutlineEntry> fallbackEntries = buildFallbackEntries(lines);
    if (fallbackEntries.isEmpty()) {
      return MarkdownOutlineModel.empty();
    }
    return new MarkdownOutlineModel(fallbackEntries, 0, false, lines.length);
  }

  /**
   * Projects the sparse heading/fallback anchors onto one outline entry per source line so the
   * shared minimap layout planner (which buckets rows by entry index) positions painted rows
   * proportionally to their real position in the document instead of evenly spreading out the small
   * number of anchors across the whole canvas height.
   */
  public JsonOutlineModel toMinimapModel(MarkdownOutlineModel model) {
    if (model == null || model.emptyModel()) {
      return JsonOutlineModel.empty();
    }

    List<JsonOutlineEntry> entries = new ArrayList<>(model.totalLines());
    int nextAnchorIndex = 0;
    int carriedDepth = 0;
    for (int lineIndex = 0; lineIndex < model.totalLines(); lineIndex++) {
      MarkdownOutlineEntry anchor =
          nextAnchorIndex < model.entries().size()
                  && model.entries().get(nextAnchorIndex).sourceLineIndex() == lineIndex
              ? model.entries().get(nextAnchorIndex++)
              : null;
      if (anchor != null) {
        carriedDepth = anchor.depth();
        entries.add(
            new JsonOutlineEntry(
                anchor.depth(),
                anchor.visualWeight(),
                anchor.fallback() ? JsonOutlineEntryKind.VALUE : JsonOutlineEntryKind.OBJECT,
                0));
      } else {
        entries.add(
            new JsonOutlineEntry(
                carriedDepth, FILLER_VISUAL_WEIGHT, JsonOutlineEntryKind.VALUE, 0));
      }
    }
    return new JsonOutlineModel(entries, model.maxDepth());
  }

  /**
   * Resolves the source line to scroll to for a pointer landing on the given minimap row bucket.
   * The bucket bounds are line indexes (see {@link #toMinimapModel}), so the target is simply the
   * midpoint of that line range clamped to the document bounds.
   */
  public int anchorLineForPointer(
      MarkdownOutlineModel model, int entryStartIndex, int entryEndIndex) {
    if (model == null || model.emptyModel() || model.totalLines() <= 0) {
      return 0;
    }
    int maxLineIndex = model.totalLines() - 1;
    int clampedStart = Math.max(0, Math.min(entryStartIndex, maxLineIndex));
    int clampedEnd = Math.max(clampedStart + 1, Math.min(entryEndIndex, model.totalLines()));
    return clampedStart + ((clampedEnd - clampedStart - 1) / 2);
  }

  private List<MarkdownOutlineEntry> buildHeadingEntries(String[] lines) {
    List<MarkdownOutlineEntry> entries = new ArrayList<>();
    boolean fencedCode = false;
    for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      String line = lines[lineIndex];
      if (FENCE_PATTERN.matcher(line).matches()) {
        fencedCode = !fencedCode;
        continue;
      }
      if (fencedCode) {
        continue;
      }
      Matcher matcher = HEADING_PATTERN.matcher(line);
      if (!matcher.matches()) {
        continue;
      }
      String title = matcher.group(2).trim();
      int depth = Math.max(0, matcher.group(1).length() - 1);
      int visualWeight = Math.max(10, Math.min(28, title.length() + 8));
      entries.add(new MarkdownOutlineEntry(title, depth, lineIndex, visualWeight, false));
    }
    return entries;
  }

  private List<MarkdownOutlineEntry> buildFallbackEntries(String[] lines) {
    List<MarkdownOutlineEntry> entries = new ArrayList<>();
    int nonEmptyCount = 0;
    for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      String line = lines[lineIndex];
      if (line == null || line.isBlank()) {
        continue;
      }
      if (nonEmptyCount % FALLBACK_SEGMENT_SIZE == 0) {
        int visualWeight = Math.max(8, Math.min(20, line.trim().length()));
        entries.add(
            new MarkdownOutlineEntry("Line " + (lineIndex + 1), 0, lineIndex, visualWeight, true));
      }
      nonEmptyCount++;
    }
    return entries;
  }
}
