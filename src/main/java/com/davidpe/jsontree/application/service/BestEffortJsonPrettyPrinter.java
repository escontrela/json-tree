package com.davidpe.jsontree.application.service;

import org.springframework.stereotype.Component;

/**
 * Applies a deterministic best-effort JSON formatting pass to incomplete large-preview chunks.
 *
 * <p>The formatter does not try to invent missing structure or guarantee valid JSON. It preserves
 * string contents and escaping while adding indentation, line breaks after commas, and spacing
 * around colons for structural readability.
 */
@Component
public class BestEffortJsonPrettyPrinter {

  private static final String INDENT = "  ";

  public String prettyPrint(String rawJson) {
    if (rawJson == null || rawJson.isEmpty()) {
      return "";
    }

    StringBuilder formatted = new StringBuilder(rawJson.length() + Math.max(16, rawJson.length() / 4));
    int indentLevel = 0;
    boolean insideString = false;
    boolean escaping = false;

    for (int index = 0; index < rawJson.length(); index++) {
      char current = rawJson.charAt(index);
      if (insideString) {
        formatted.append(current);
        if (escaping) {
          escaping = false;
        } else if (current == '\\') {
          escaping = true;
        } else if (current == '"') {
          insideString = false;
        }
        continue;
      }

      if (Character.isWhitespace(current)) {
        continue;
      }

      if (current == '"') {
        insideString = true;
        formatted.append(current);
        continue;
      }

      switch (current) {
        case '{', '[' -> {
          formatted.append(current);
          if (!nextNonWhitespaceEquals(rawJson, index + 1, matchingCloser(current))) {
            formatted.append('\n');
            indentLevel++;
            appendIndent(formatted, indentLevel);
          }
        }
        case '}', ']' -> {
          if (!endsWithOpeningDelimiter(formatted)) {
            formatted.append('\n');
            indentLevel = Math.max(0, indentLevel - 1);
            appendIndent(formatted, indentLevel);
          }
          formatted.append(current);
        }
        case ',' -> {
          formatted.append(current).append('\n');
          appendIndent(formatted, indentLevel);
        }
        case ':' -> formatted.append(" : ");
        default -> formatted.append(current);
      }
    }

    return formatted.toString();
  }

  private void appendIndent(StringBuilder formatted, int indentLevel) {
    for (int level = 0; level < indentLevel; level++) {
      formatted.append(INDENT);
    }
  }

  private boolean nextNonWhitespaceEquals(String text, int startIndex, char expected) {
    for (int index = startIndex; index < text.length(); index++) {
      char current = text.charAt(index);
      if (Character.isWhitespace(current)) {
        continue;
      }
      return current == expected;
    }
    return false;
  }

  private boolean endsWithOpeningDelimiter(StringBuilder formatted) {
    for (int index = formatted.length() - 1; index >= 0; index--) {
      char current = formatted.charAt(index);
      if (Character.isWhitespace(current)) {
        continue;
      }
      return current == '{' || current == '[';
    }
    return false;
  }

  private char matchingCloser(char opener) {
    return opener == '{' ? '}' : ']';
  }
}
