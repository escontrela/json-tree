package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.springframework.stereotype.Component;

@Component
public class AsciiTreeSyntaxHighlighter {

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^([│├└─ ]*)(.*)$");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(.+?):\\s(.+)$");
    private static final Pattern ARRAY_LABEL_PATTERN = Pattern.compile("^(.+)\\s(\\[\\d+])$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");

    public TextFlow highlight(AsciiTreeDocument document) {
        TextFlow textFlow = new TextFlow();
        textFlow.getStyleClass().add("tree-content");
        textFlow.getStyleClass().add("tree-content-flow");

        for (StyledSegment segment : tokenize(document)) {
            textFlow.getChildren().add(styledText(segment.text(), segment.styleClass()));
        }
        return textFlow;
    }

    List<StyledSegment> tokenize(AsciiTreeDocument document) {
        List<StyledSegment> segments = new ArrayList<>();
        String[] lines = document.content().split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            segments.addAll(tokenizeLine(lines[index]));
            if (index < lines.length - 1) {
                segments.add(new StyledSegment("\n", "tree-default"));
            }
        }
        return segments;
    }

    private List<StyledSegment> tokenizeLine(String line) {
        List<StyledSegment> segments = new ArrayList<>();
        Matcher prefixMatcher = PREFIX_PATTERN.matcher(line);
        if (!prefixMatcher.matches()) {
            segments.add(new StyledSegment(line, "tree-default"));
            return segments;
        }

        String prefix = prefixMatcher.group(1);
        String payload = prefixMatcher.group(2);
        segments.add(new StyledSegment(prefix, "tree-prefix"));

        if (payload.isBlank()) {
            return segments;
        }

        Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(payload);
        if (keyValueMatcher.matches()) {
            segments.add(new StyledSegment(keyValueMatcher.group(1), "tree-key"));
            segments.add(new StyledSegment(": ", "tree-default"));
            segments.add(new StyledSegment(keyValueMatcher.group(2), valueStyleClass(keyValueMatcher.group(2))));
            return segments;
        }

        Matcher arrayLabelMatcher = ARRAY_LABEL_PATTERN.matcher(payload);
        if (arrayLabelMatcher.matches()) {
            segments.add(new StyledSegment(arrayLabelMatcher.group(1), "tree-structure"));
            segments.add(new StyledSegment(" ", "tree-default"));
            segments.add(new StyledSegment(arrayLabelMatcher.group(2), "tree-array-count"));
            return segments;
        }

        segments.add(new StyledSegment(payload, "tree-structure"));
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

    private Text styledText(String text, String styleClass) {
        Text node = new Text(text);
        node.getStyleClass().add(styleClass);
        return node;
    }

    record StyledSegment(String text, String styleClass) {
    }
}
