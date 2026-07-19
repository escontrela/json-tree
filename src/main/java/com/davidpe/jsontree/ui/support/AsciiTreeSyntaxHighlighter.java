package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AsciiTreeSyntaxHighlighter {

  private static final Pattern PREFIX_PATTERN = Pattern.compile("^([│├└─ ]*)(.*)$");
  private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(.+?):\\s(.+)$");
  private static final Pattern ARRAY_LABEL_PATTERN = Pattern.compile("^(.+)\\s(\\[\\d+])$");
  private static final Pattern NUMBER_PATTERN =
      Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");

  private final SearchHighlightRangeNormalizer highlightRangeNormalizer;

  public AsciiTreeSyntaxHighlighter() {
    this(new LargePreviewProperties(), new SearchHighlightRangeNormalizer());
  }

  public AsciiTreeSyntaxHighlighter(LargePreviewProperties largePreviewProperties) {
    this(largePreviewProperties, new SearchHighlightRangeNormalizer());
  }

  @Autowired
  AsciiTreeSyntaxHighlighter(
      LargePreviewProperties largePreviewProperties,
      SearchHighlightRangeNormalizer highlightRangeNormalizer) {
    this.highlightRangeNormalizer = highlightRangeNormalizer;
  }

  public ViewerTextRenderPlan buildRenderPlan(
      AsciiTreeDocument document, List<SearchHighlightRange> highlightRanges) {
    if (document.content().isEmpty()) {
      return ViewerTextRenderPlan.normal(List.of());
    }
    List<SearchHighlightRange> orderedRanges = highlightRangeNormalizer.normalize(highlightRanges);

    List<ViewerTextRenderFragment> fragments = new ArrayList<>();
    int cursor = 0;
    int rangeIndex = 0;
    int lineStart = 0;
    String content = document.content();
    for (int index = 0; index <= content.length(); index++) {
      boolean endOfLine = index == content.length() || content.charAt(index) == '\n';
      if (!endOfLine) {
        continue;
      }

      String line = content.substring(lineStart, index);
      for (StyledSegment segment : tokenizeLine(line)) {
        int segmentStart = cursor;
        int segmentEnd = cursor + segment.text().length();
        int localCursor = segmentStart;

        while (rangeIndex < orderedRanges.size()
            && orderedRanges.get(rangeIndex).endIndex() <= segmentStart) {
          rangeIndex++;
        }

        int activeRangeIndex = rangeIndex;
        while (activeRangeIndex < orderedRanges.size()) {
          SearchHighlightRange range = orderedRanges.get(activeRangeIndex);
          if (range.startIndex() >= segmentEnd) {
            break;
          }

          int overlapStart = Math.max(segmentStart, range.startIndex());
          int overlapEnd = Math.min(segmentEnd, range.endIndex());
          if (overlapStart > localCursor) {
            fragments.add(
                fragmentForSegment(
                    sliceSegment(segment, segmentStart, localCursor, overlapStart), false, false));
          }
          if (overlapEnd > overlapStart) {
            fragments.add(
                fragmentForSegment(
                    sliceSegment(segment, segmentStart, overlapStart, overlapEnd),
                    true,
                    range.active()));
          }
          localCursor = Math.max(localCursor, overlapEnd);
          if (range.endIndex() <= segmentEnd) {
            activeRangeIndex++;
            continue;
          }
          break;
        }

        if (localCursor < segmentEnd) {
          fragments.add(
              fragmentForSegment(
                  sliceSegment(segment, segmentStart, localCursor, segmentEnd), false, false));
        }
        cursor = segmentEnd;
      }

      if (index < content.length()) {
        fragments.add(fragmentForSegment(styledSegment("\n", "tree-default"), false, false));
        cursor++;
        lineStart = index + 1;
      }
    }
    return ViewerTextRenderPlan.normal(fragments);
  }

  private StyledSegment sliceSegment(
      StyledSegment segment,
      int segmentStart,
      int sliceStart,
      int sliceEnd
  ) {
    int localStart = sliceStart - segmentStart;
    int localEnd = sliceEnd - segmentStart;
    return new StyledSegment(
        segment.text().substring(localStart, localEnd),
        segment.styleClass(),
        segment.colorHex());
  }

  private ViewerTextRenderFragment fragmentForSegment(
      StyledSegment segment, boolean highlighted, boolean activeHighlight) {
    return new ViewerTextRenderFragment(
        segment.text(), segment.styleClass(), segment.colorHex(), highlighted, activeHighlight);
  }

  List<StyledSegment> tokenize(AsciiTreeDocument document) {
    List<StyledSegment> segments = new ArrayList<>();
    String[] lines = document.content().split("\\R", -1);
    for (int index = 0; index < lines.length; index++) {
      segments.addAll(tokenizeLine(lines[index]));
      if (index < lines.length - 1) {
        segments.add(styledSegment("\n", "tree-default"));
      }
    }
    return segments;
  }

  private List<StyledSegment> tokenizeLine(String line) {
    List<StyledSegment> segments = new ArrayList<>();
    Matcher prefixMatcher = PREFIX_PATTERN.matcher(line);
    if (!prefixMatcher.matches()) {
      segments.add(styledSegment(line, "tree-default"));
      return segments;
    }

    String prefix = prefixMatcher.group(1);
    String payload = prefixMatcher.group(2);
    segments.add(styledSegment(prefix, "tree-prefix"));

    if (payload.isBlank()) {
      return segments;
    }

    Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(payload);
    if (keyValueMatcher.matches()) {
      segments.add(styledSegment(keyValueMatcher.group(1), "tree-key"));
      segments.add(styledSegment(": ", "tree-default"));
      segments.add(
          styledSegment(keyValueMatcher.group(2), valueStyleClass(keyValueMatcher.group(2))));
      return segments;
    }

    Matcher arrayLabelMatcher = ARRAY_LABEL_PATTERN.matcher(payload);
    if (arrayLabelMatcher.matches()) {
      segments.add(styledSegment(arrayLabelMatcher.group(1), "tree-structure"));
      segments.add(styledSegment(" ", "tree-default"));
      segments.add(styledSegment(arrayLabelMatcher.group(2), "tree-array-count"));
      return segments;
    }

    segments.add(styledSegment(payload, "tree-structure"));
    return segments;
  }

  private String valueStyleClass(String value) {
    if (value.startsWith("\"") && value.endsWith("\"")) {
      return "tree-string";
    }
    if ("true".equals(value) || "false".equals(value)) {
      return "tree-boolean";
    }
    if ("null".equals(value)) {
      return "tree-null";
    }
    if (NUMBER_PATTERN.matcher(value).matches()) {
      return "tree-number";
    }
    return "tree-default";
  }

  private StyledSegment styledSegment(String text, String styleClass) {
    return new StyledSegment(text, styleClass, colorFor(styleClass));
  }

  private String colorFor(String styleClass) {
    return switch (styleClass) {
      case "tree-prefix" -> "#6f7482";
      case "tree-structure" -> "#355c8a";
      case "tree-array-count" -> "#8fd3ff";
      case "tree-key" -> "#9a6a00";
      case "tree-string" -> "#1d8f5f";
      case "tree-number" -> "#1d8f5f";
      case "tree-boolean" -> "#1d8f5f";
      case "tree-null" -> "#1d8f5f";
      default -> "#d9dce3";
    };
  }

  record StyledSegment(String text, String styleClass, String colorHex) {}
}
