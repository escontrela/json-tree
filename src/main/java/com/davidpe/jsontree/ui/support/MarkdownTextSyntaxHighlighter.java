package com.davidpe.jsontree.ui.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Produces lightweight Markdown-aware spans while preserving raw source text exactly as loaded.
 */
@Component
public class MarkdownTextSyntaxHighlighter {

  private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,6}\\s+.+$");
  private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s?.*$");
  private static final Pattern LIST_PATTERN = Pattern.compile("^([*-]|\\d+\\.)\\s+.+$");
  private static final Pattern FENCE_PATTERN = Pattern.compile("^```.*$");

  public ViewerTextRenderPlan buildRenderPlan(String content) {
    if (content == null || content.isEmpty()) {
      return ViewerTextRenderPlan.normal(List.of());
    }

    List<ViewerTextRenderFragment> fragments = new ArrayList<>();
    String[] lines = content.split("(?<=\\n)", -1);
    boolean fencedCode = false;
    for (String line : lines) {
      String normalizedLine = line.endsWith("\n") ? line.substring(0, line.length() - 1) : line;
      String styleClass = "raw-json-text";
      if (FENCE_PATTERN.matcher(normalizedLine).matches()) {
        styleClass = "markdown-fence";
        fencedCode = !fencedCode;
      } else if (!fencedCode && HEADING_PATTERN.matcher(normalizedLine).matches()) {
        styleClass = "markdown-heading";
      } else if (!fencedCode && BLOCKQUOTE_PATTERN.matcher(normalizedLine).matches()) {
        styleClass = "markdown-quote";
      } else if (!fencedCode && LIST_PATTERN.matcher(normalizedLine).matches()) {
        styleClass = "markdown-list";
      }
      fragments.add(new ViewerTextRenderFragment(line, styleClass, "#2d333a", false, false));
    }
    return ViewerTextRenderPlan.normal(fragments);
  }
}
