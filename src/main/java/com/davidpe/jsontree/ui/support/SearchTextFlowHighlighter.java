package com.davidpe.jsontree.ui.support;

import java.util.Comparator;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.springframework.stereotype.Component;

@Component
public class SearchTextFlowHighlighter {

  private static final String INACTIVE_HIGHLIGHT_COLOR = "#355c8a";
  private static final String ACTIVE_HIGHLIGHT_COLOR = "#1c69d4";

  public void appendHighlightedText(
      TextFlow textFlow,
      String content,
      List<SearchHighlightRange> highlightRanges,
      String baseStyleClass,
      String baseColorHex
  ) {
    textFlow.getChildren().clear();
    if (content == null || content.isEmpty()) {
      return;
    }

    List<SearchHighlightRange> orderedRanges =
        highlightRanges.stream()
            .sorted(Comparator.comparingInt(SearchHighlightRange::startIndex))
            .toList();

    int cursor = 0;
    for (SearchHighlightRange range : orderedRanges) {
      if (range.startIndex() > cursor) {
        textFlow.getChildren().add(buildText(content.substring(cursor, range.startIndex()), baseStyleClass, baseColorHex));
      }
      if (range.endIndex() > range.startIndex()) {
        textFlow.getChildren().add(
            buildHighlightedText(
                content.substring(range.startIndex(), range.endIndex()),
                baseStyleClass,
                range.active()));
      }
      cursor = Math.max(cursor, range.endIndex());
    }

    if (cursor < content.length()) {
      textFlow.getChildren().add(buildText(content.substring(cursor), baseStyleClass, baseColorHex));
    }
  }

  private Text buildText(String textValue, String baseStyleClass, String colorHex) {
    Text node = new Text(textValue);
    node.getStyleClass().add(baseStyleClass);
    node.setFill(Color.web(colorHex));
    return node;
  }

  private Text buildHighlightedText(String textValue, String baseStyleClass, boolean active) {
    Text node = buildText(textValue, baseStyleClass, active ? ACTIVE_HIGHLIGHT_COLOR : INACTIVE_HIGHLIGHT_COLOR);
    node.getStyleClass().add("search-match");
    if (active) {
      node.getStyleClass().add("search-match-active");
      node.setStyle("-fx-font-weight: 700;");
    }
    node.setUnderline(true);
    return node;
  }
}
