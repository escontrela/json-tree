package com.davidpe.jsontree.ui.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Builds a lightweight interpreted Markdown render plan for the shared RichTextFX viewer.
 *
 * <p>The renderer intentionally stays deterministic and line-oriented: each source line produces a
 * corresponding rendered line so scrolling and auxiliary viewport state remain predictable while
 * the reading surface becomes easier to scan than raw source.
 */
@Component
public class RenderedMarkdownTextRenderer {

  private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
  private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)(\\d+)\\.\\s+(.*)$");
  private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)[-*+]\\s+(.*)$");
  private static final Pattern BLOCK_QUOTE_PATTERN = Pattern.compile("^(>+)\\s?(.*)$");
  private static final Pattern FENCE_PATTERN = Pattern.compile("^```\\s*(.*)$");

  public ViewerTextRenderPlan buildRenderPlan(String content) {
    if (content == null || content.isEmpty()) {
      return ViewerTextRenderPlan.normal(List.of());
    }

    List<ViewerTextRenderFragment> fragments = new ArrayList<>();
    String[] lines = content.split("(?<=\\n)", -1);
    boolean fencedCode = false;

    for (String line : lines) {
      String renderedLine = line;
      String normalizedLine = line.endsWith("\n") ? line.substring(0, line.length() - 1) : line;
      String suffix = line.endsWith("\n") ? "\n" : "";
      String styleClass = "markdown-rendered-paragraph";

      Matcher fenceMatcher = FENCE_PATTERN.matcher(normalizedLine);
      if (fenceMatcher.matches()) {
        boolean openingFence = !fencedCode;
        fencedCode = !fencedCode;
        String info = fenceMatcher.group(1) == null ? "" : fenceMatcher.group(1).trim();
        renderedLine =
            (openingFence
                    ? (info.isBlank() ? "code block" : "code block (" + info + ")")
                    : "end code block")
                + suffix;
        styleClass = "markdown-rendered-code-fence";
      } else if (fencedCode) {
        styleClass = "markdown-rendered-code";
      } else {
        Matcher headingMatcher = HEADING_PATTERN.matcher(normalizedLine);
        Matcher orderedListMatcher = ORDERED_LIST_PATTERN.matcher(normalizedLine);
        Matcher unorderedListMatcher = UNORDERED_LIST_PATTERN.matcher(normalizedLine);
        Matcher blockQuoteMatcher = BLOCK_QUOTE_PATTERN.matcher(normalizedLine);
        if (headingMatcher.matches()) {
          renderedLine = headingMatcher.group(2).trim() + suffix;
          styleClass = headingStyleClass(headingMatcher.group(1).length());
        } else if (orderedListMatcher.matches()) {
          renderedLine =
              orderedListMatcher.group(1)
                  + orderedListMatcher.group(2)
                  + ". "
                  + orderedListMatcher.group(3)
                  + suffix;
          styleClass = "markdown-rendered-list";
        } else if (unorderedListMatcher.matches()) {
          renderedLine =
              unorderedListMatcher.group(1) + "• " + unorderedListMatcher.group(2) + suffix;
          styleClass = "markdown-rendered-list";
        } else if (blockQuoteMatcher.matches()) {
          int quoteDepth = blockQuoteMatcher.group(1).length();
          renderedLine =
              "│ ".repeat(Math.max(1, quoteDepth)) + blockQuoteMatcher.group(2).trim() + suffix;
          styleClass = "markdown-rendered-quote";
        } else if (normalizedLine.isBlank()) {
          styleClass = "markdown-rendered-paragraph";
        }
      }

      fragments.add(new ViewerTextRenderFragment(renderedLine, styleClass, "#2d333a", false, false));
    }

    return ViewerTextRenderPlan.normal(fragments);
  }

  private String headingStyleClass(int level) {
    return switch (level) {
      case 1 -> "markdown-rendered-heading-1";
      case 2 -> "markdown-rendered-heading-2";
      default -> "markdown-rendered-heading-3";
    };
  }
}
