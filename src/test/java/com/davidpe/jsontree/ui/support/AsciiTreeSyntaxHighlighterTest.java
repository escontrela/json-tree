package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void keepsStyledFragmentsEvenWhenTheLegacyBudgetWouldHaveBeenExceeded() {
        LargePreviewProperties properties = new LargePreviewProperties();
        AsciiTreeSyntaxHighlighter guardedHighlighter = new AsciiTreeSyntaxHighlighter(properties);
        AsciiTreeDocument document = new AsciiTreeDocument("root", "root\n├─ a: 1\n└─ b: 2", 3);

        ViewerTextRenderPlan renderPlan = guardedHighlighter.buildRenderPlan(document, List.of());

        assertFalse(renderPlan.guardrailApplied());
        assertTrue(renderPlan.fragments().size() > 1);
        assertEquals(
                document.content(),
                renderPlan.fragments().stream()
                        .map(ViewerTextRenderFragment::text)
                        .reduce("", String::concat));
    }

    @Test
    void normalizesOverlappingInactiveAndActiveHighlightRanges() {
        AsciiTreeDocument document = new AsciiTreeDocument("root", "root\n└─ name: \"David\"", 2);

        ViewerTextRenderPlan renderPlan = highlighter.buildRenderPlan(
                document,
                List.of(
                        new SearchHighlightRange(10, 17, false),
                        new SearchHighlightRange(13, 19, true))
        );

        String renderedText = renderPlan.fragments().stream()
                .map(ViewerTextRenderFragment::text)
                .reduce("", String::concat);

        assertEquals(document.content(), renderedText);
        assertTrue(renderPlan.fragments().stream()
                .filter(ViewerTextRenderFragment::highlighted)
                .anyMatch(ViewerTextRenderFragment::activeHighlight));
        assertEquals(
                9,
                renderPlan.fragments().stream()
                        .filter(ViewerTextRenderFragment::highlighted)
                        .mapToInt(fragment -> fragment.text().length())
                        .sum());
        assertEquals(
                6,
                renderPlan.fragments().stream()
                        .filter(ViewerTextRenderFragment::activeHighlight)
                        .mapToInt(fragment -> fragment.text().length())
                        .sum());
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
