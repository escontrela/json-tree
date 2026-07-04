package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.springframework.stereotype.Component;

@Component
public class AsciiTreeSyntaxHighlighter {

  private static final Pattern PREFIX_PATTERN = Pattern.compile("^([│├└─ ]*)(.*)$");
  private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(.+?):\\s(.+)$");
  private static final Pattern ARRAY_LABEL_PATTERN = Pattern.compile("^(.+)\\s(\\[\\d+])$");
  private static final Pattern NUMBER_PATTERN =
      Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");

  public TextFlow highlight(AsciiTreeDocument document) {
    TextFlow textFlow = new TextFlow();
    textFlow.getStyleClass().add("tree-content");
    textFlow.getStyleClass().add("tree-content-flow");

    appendHighlightedContent(textFlow, document);
    return textFlow;
  }

  public void appendHighlightedContent(TextFlow textFlow, AsciiTreeDocument document) {
    appendHighlightedContent(textFlow, document, List.of());
  }

  public void appendHighlightedContent(
      TextFlow textFlow,
      AsciiTreeDocument document,
      List<SearchHighlightRange> highlightRanges
  ) {
    textFlow.getChildren().clear();
    List<SearchHighlightRange> orderedRanges =
        highlightRanges.stream()
            .sorted(Comparator.comparingInt(SearchHighlightRange::startIndex))
            .toList();

    int cursor = 0;
    for (StyledSegment segment : tokenize(document)) {
      int segmentStart = cursor;
      int segmentEnd = cursor + segment.text().length();
      int localCursor = segmentStart;

      for (SearchHighlightRange range : orderedRanges) {
        if (range.endIndex() <= segmentStart) {
          continue;
        }
        if (range.startIndex() >= segmentEnd) {
          break;
        }

        int overlapStart = Math.max(segmentStart, range.startIndex());
        int overlapEnd = Math.min(segmentEnd, range.endIndex());
        if (overlapStart > localCursor) {
          textFlow.getChildren().add(styledText(sliceSegment(segment, segmentStart, localCursor, overlapStart), false, false));
        }
        if (overlapEnd > overlapStart) {
          textFlow.getChildren().add(styledText(sliceSegment(segment, segmentStart, overlapStart, overlapEnd), true, range.active()));
        }
        localCursor = Math.max(localCursor, overlapEnd);
      }

      if (localCursor < segmentEnd) {
        textFlow.getChildren().add(styledText(sliceSegment(segment, segmentStart, localCursor, segmentEnd), false, false));
      }
      cursor = segmentEnd;
    }
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

  private Text styledText(StyledSegment segment, boolean highlighted, boolean activeHighlight) {
    Text node = new Text(segment.text());
    node.getStyleClass().add(segment.styleClass());
    if (highlighted) {
      node.getStyleClass().add("search-match");
      if (activeHighlight) {
        node.getStyleClass().add("search-match-active");
        node.setFill(Color.web("#1c69d4"));
        node.setStyle("-fx-font-weight: 700;");
      } else {
        node.setFill(Color.web("#355c8a"));
      }
      node.setUnderline(true);
      return node;
    }
    node.setFill(Color.web(segment.colorHex()));
    return node;
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
