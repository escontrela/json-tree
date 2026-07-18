package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.util.ArrayList;
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

  private final int textNodeBudget;
  private final SearchHighlightRangeNormalizer highlightRangeNormalizer;

  public AsciiTreeSyntaxHighlighter() {
    this(new LargePreviewProperties(), new SearchHighlightRangeNormalizer());
  }

  public AsciiTreeSyntaxHighlighter(LargePreviewProperties largePreviewProperties) {
    this(largePreviewProperties, new SearchHighlightRangeNormalizer());
  }

  AsciiTreeSyntaxHighlighter(
      LargePreviewProperties largePreviewProperties,
      SearchHighlightRangeNormalizer highlightRangeNormalizer) {
    this.textNodeBudget = Math.max(1, largePreviewProperties.getTextNodeBudget());
    this.highlightRangeNormalizer = highlightRangeNormalizer;
  }

  public TextFlow highlight(AsciiTreeDocument document) {
    TextFlow textFlow = new TextFlow();
    textFlow.getStyleClass().add("tree-content");
    textFlow.getStyleClass().add("tree-content-flow");

    appendHighlightedContent(textFlow, document);
    return textFlow;
  }

  public TextFlowRenderOutcome appendHighlightedContent(TextFlow textFlow, AsciiTreeDocument document) {
    return appendHighlightedContent(textFlow, document, List.of());
  }

  public TextFlowRenderOutcome appendHighlightedContent(
      TextFlow textFlow,
      AsciiTreeDocument document,
      List<SearchHighlightRange> highlightRanges
  ) {
    textFlow.getChildren().clear();
    TextFlowRenderPlan renderPlan = buildRenderPlan(document, highlightRanges);
    for (TextFlowRenderFragment fragment : renderPlan.fragments()) {
      textFlow.getChildren().add(renderFragment(fragment));
    }
    return renderPlan.guardrailApplied()
        ? TextFlowRenderOutcome.guardrailTriggered()
        : TextFlowRenderOutcome.normalOutcome();
  }

  public TextFlowRenderPlan buildRenderPlan(
      AsciiTreeDocument document, List<SearchHighlightRange> highlightRanges) {
    if (document.content().isEmpty()) {
      return TextFlowRenderPlan.normal(List.of());
    }
    List<SearchHighlightRange> orderedRanges = highlightRangeNormalizer.normalize(highlightRanges);

    List<TextFlowRenderFragment> fragments = new ArrayList<>();
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
          if (overlapStart > localCursor
              && appendFragment(
                  fragments,
                  fragmentForSegment(
                      sliceSegment(segment, segmentStart, localCursor, overlapStart), false, false))) {
            return applyPlainTextFallback(content, "tree-default", "#d9dce3");
          }
          if (overlapEnd > overlapStart
              && appendFragment(
                  fragments,
                  fragmentForSegment(
                      sliceSegment(segment, segmentStart, overlapStart, overlapEnd),
                      true,
                      range.active()))) {
            return applyPlainTextFallback(content, "tree-default", "#d9dce3");
          }
          localCursor = Math.max(localCursor, overlapEnd);
          if (range.endIndex() <= segmentEnd) {
            activeRangeIndex++;
            continue;
          }
          break;
        }

        if (localCursor < segmentEnd
            && appendFragment(
                fragments,
                fragmentForSegment(
                    sliceSegment(segment, segmentStart, localCursor, segmentEnd), false, false))) {
          return applyPlainTextFallback(content, "tree-default", "#d9dce3");
        }
        cursor = segmentEnd;
      }

      if (index < content.length()) {
        if (appendFragment(
            fragments, fragmentForSegment(styledSegment("\n", "tree-default"), false, false))) {
          return applyPlainTextFallback(content, "tree-default", "#d9dce3");
        }
        cursor++;
        lineStart = index + 1;
      }
    }
    return TextFlowRenderPlan.normal(fragments);
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

  private Text renderFragment(TextFlowRenderFragment fragment) {
    Text node = new Text(fragment.text());
    node.getStyleClass().add(fragment.styleClass());
    if (fragment.highlighted()) {
      node.getStyleClass().add("search-match");
      if (fragment.activeHighlight()) {
        node.getStyleClass().add("search-match-active");
        node.setFill(Color.web("#1c69d4"));
        node.setStyle("-fx-font-weight: 700;");
      } else {
        node.setFill(Color.web("#355c8a"));
      }
      node.setUnderline(true);
      return node;
    }
    node.setFill(Color.web(fragment.colorHex()));
    return node;
  }

  private TextFlowRenderFragment fragmentForSegment(
      StyledSegment segment, boolean highlighted, boolean activeHighlight) {
    return new TextFlowRenderFragment(
        segment.text(), segment.styleClass(), segment.colorHex(), highlighted, activeHighlight);
  }

  private boolean appendFragment(
      List<TextFlowRenderFragment> fragments, TextFlowRenderFragment fragment) {
    if (fragments.size() >= textNodeBudget) {
      return true;
    }
    fragments.add(fragment);
    return false;
  }

  private TextFlowRenderPlan applyPlainTextFallback(
      String content, String baseStyleClass, String baseColorHex) {
    return TextFlowRenderPlan.guardrailFallback(content, baseStyleClass, baseColorHex);
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
