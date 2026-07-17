package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Estimates whether a FULL ASCII tree render would overflow the JavaFX text-node budget.
 *
 * <p>The guard mirrors the syntax-highlighting fragment shape closely enough to promote unstable
 * FULL loads into {@code LARGE_PREVIEW} before the UI falls back to the grey simplified tree.
 */
@Service
public class AsciiTreeFullRenderGuard {

  private static final Pattern PREFIX_PATTERN = Pattern.compile("^([│├└─ ]*)(.*)$");
  private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(.+?):\\s(.+)$");
  private static final Pattern ARRAY_LABEL_PATTERN = Pattern.compile("^(.+)\\s(\\[\\d+])$");

  private final int textNodeBudget;

  @Autowired
  public AsciiTreeFullRenderGuard(LargePreviewProperties largePreviewProperties) {
    this(Math.max(1, largePreviewProperties.getTextNodeBudget()));
  }

  AsciiTreeFullRenderGuard(int textNodeBudget) {
    this.textNodeBudget = Math.max(1, textNodeBudget);
  }

  public boolean exceedsBudget(AsciiTreeDocument document) {
    return estimateFragmentCount(document) > textNodeBudget;
  }

  int estimateFragmentCount(AsciiTreeDocument document) {
    if (document == null || document.content().isEmpty()) {
      return 0;
    }

    String content = document.content();
    int fragments = 0;
    int lineStart = 0;
    for (int index = 0; index <= content.length(); index++) {
      boolean endOfLine = index == content.length() || content.charAt(index) == '\n';
      if (!endOfLine) {
        continue;
      }

      fragments += estimateLineFragments(content.substring(lineStart, index));
      if (fragments > textNodeBudget) {
        return fragments;
      }

      if (index < content.length()) {
        fragments++;
        if (fragments > textNodeBudget) {
          return fragments;
        }
        lineStart = index + 1;
      }
    }
    return fragments;
  }

  private int estimateLineFragments(String line) {
    Matcher prefixMatcher = PREFIX_PATTERN.matcher(line);
    if (!prefixMatcher.matches()) {
      return 1;
    }

    int fragments = 1;
    String payload = prefixMatcher.group(2);
    if (payload.isBlank()) {
      return fragments;
    }
    if (KEY_VALUE_PATTERN.matcher(payload).matches()) {
      return fragments + 3;
    }
    if (ARRAY_LABEL_PATTERN.matcher(payload).matches()) {
      return fragments + 2;
    }
    return fragments + 1;
  }
}
