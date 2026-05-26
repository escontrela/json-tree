package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class AsciiTreeSyntaxHighlighterTest {

    private final AsciiTreeSyntaxHighlighter highlighter = new AsciiTreeSyntaxHighlighter();

    @Test
    void highlightsKeysValuesAndStructuralLabelsWithoutChangingContent() {
        AsciiTreeDocument document = new AsciiTreeDocument(
                "root",
                """
                root
                ├─ user
                │  ├─ id: 42
                │  ├─ name: "David"
                │  └─ active: true
                └─ tags [2]""",
                6
        );

        List<AsciiTreeSyntaxHighlighter.StyledSegment> segments = highlighter.tokenize(document);

        String renderedText = segments.stream()
                .map(AsciiTreeSyntaxHighlighter.StyledSegment::text)
                .reduce("", String::concat);

        assertTrue(renderedText.contains("name: \"David\""));
        assertTrue(segments.stream().anyMatch(segment -> "tree-key".equals(segment.styleClass())));
        assertTrue(segments.stream().anyMatch(segment -> "tree-string".equals(segment.styleClass())));
        assertTrue(segments.stream().anyMatch(segment -> "tree-number".equals(segment.styleClass())));
        assertTrue(segments.stream().anyMatch(segment -> "tree-boolean".equals(segment.styleClass())));
        assertTrue(segments.stream().anyMatch(segment -> "tree-array-count".equals(segment.styleClass())));
    }
}
