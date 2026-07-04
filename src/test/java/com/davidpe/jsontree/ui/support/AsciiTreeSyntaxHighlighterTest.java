package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
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
        assertEquals("#d9dce3", findSegment(segments, ": ").colorHex());
        assertEquals("#9a6a00", findSegment(segments, "name").colorHex());
        assertEquals("#1d8f5f", findSegment(segments, "\"David\"").colorHex());
        assertEquals("#1d8f5f", findSegment(segments, "true").colorHex());
        assertEquals("#8fd3ff", findSegment(segments, "[2]").colorHex());
    }

    @Test
    void fallsBackToSinglePlainTextNodeWhenBudgetWouldBeExceeded() {
        LargePreviewProperties properties = new LargePreviewProperties();
        properties.setTextNodeBudget(2);
        AsciiTreeSyntaxHighlighter guardedHighlighter = new AsciiTreeSyntaxHighlighter(properties);
        AsciiTreeDocument document = new AsciiTreeDocument("root", "root\n├─ a: 1\n└─ b: 2", 3);

        TextFlowRenderPlan renderPlan = guardedHighlighter.buildRenderPlan(document, List.of());

        assertTrue(renderPlan.guardrailApplied());
        assertEquals(1, renderPlan.fragments().size());
        assertEquals(document.content(), renderPlan.fragments().getFirst().text());
    }

    private AsciiTreeSyntaxHighlighter.StyledSegment findSegment(
            List<AsciiTreeSyntaxHighlighter.StyledSegment> segments,
            String text
    ) {
        return segments.stream()
                .filter(segment -> text.equals(segment.text()))
                .findFirst()
                .orElseThrow();
    }
}
