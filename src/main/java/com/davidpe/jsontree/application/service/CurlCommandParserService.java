package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.CurlCommandParseResult;
import com.davidpe.jsontree.application.model.CurlCommandSource;
import com.davidpe.jsontree.application.model.CurlExecutionHeader;
import com.davidpe.jsontree.application.model.CurlExecutionRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Parses a bounded curl subset into a normalized execution request.
 */
@Service
public class CurlCommandParserService {

  public CurlCommandParseResult detectAndParse(String rawText, CurlCommandSource source) {
    if (rawText == null || rawText.isBlank()) {
      return CurlCommandParseResult.notCurl();
    }

    String normalized = normalizeContinuations(rawText).trim();
    if (!normalized.startsWith("curl ") && !normalized.equals("curl")) {
      return CurlCommandParseResult.notCurl();
    }
    if (containsUnsafeShellFeatures(normalized)) {
      return CurlCommandParseResult.invalid(
          "Unsupported shell features detected in curl command.");
    }

    List<String> tokens;
    try {
      tokens = tokenize(normalized);
    } catch (IllegalArgumentException exception) {
      return CurlCommandParseResult.invalid(exception.getMessage());
    }
    if (tokens.isEmpty() || !"curl".equals(tokens.get(0))) {
      return CurlCommandParseResult.notCurl();
    }

    boolean followRedirects = false;
    String explicitMethod = null;
    String urlToken = null;
    String body = "";
    List<CurlExecutionHeader> headers = new ArrayList<>();

    for (int index = 1; index < tokens.size(); index++) {
      String token = tokens.get(index);
      switch (token) {
        case "--location", "-L" -> followRedirects = true;
        case "--compressed", "--insecure", "-k" -> {
          // Supported as safe no-ops in the normalized Java execution path.
        }
        case "--header", "-H" -> {
          try {
            index = requireNextToken(index, tokens, token);
          } catch (IllegalArgumentException exception) {
            return CurlCommandParseResult.invalid(exception.getMessage());
          }
          String header = tokens.get(index);
          int separatorIndex = header.indexOf(':');
          if (separatorIndex <= 0) {
            return CurlCommandParseResult.invalid("Header must use the form 'Name: value'.");
          }
          headers.add(
              new CurlExecutionHeader(
                  header.substring(0, separatorIndex).trim(),
                  header.substring(separatorIndex + 1).trim()));
        }
        case "--data", "--data-raw", "--data-binary", "-d" -> {
          try {
            index = requireNextToken(index, tokens, token);
          } catch (IllegalArgumentException exception) {
            return CurlCommandParseResult.invalid(exception.getMessage());
          }
          body = tokens.get(index);
        }
        case "--request", "-X" -> {
          try {
            index = requireNextToken(index, tokens, token);
          } catch (IllegalArgumentException exception) {
            return CurlCommandParseResult.invalid(exception.getMessage());
          }
          explicitMethod = tokens.get(index).trim().toUpperCase(Locale.ROOT);
        }
        case "--url" -> {
          try {
            index = requireNextToken(index, tokens, token);
          } catch (IllegalArgumentException exception) {
            return CurlCommandParseResult.invalid(exception.getMessage());
          }
          urlToken = tokens.get(index);
        }
        default -> {
          if (token.startsWith("-")) {
            return CurlCommandParseResult.invalid("Unsupported curl option: " + token);
          }
          if (urlToken != null) {
            return CurlCommandParseResult.invalid("Only one curl URL target is supported.");
          }
          urlToken = token;
        }
      }
    }

    if (urlToken == null || urlToken.isBlank()) {
      return CurlCommandParseResult.invalid("Curl command must include a target URL.");
    }

    URI url;
    try {
      url = URI.create(urlToken);
    } catch (IllegalArgumentException exception) {
      return CurlCommandParseResult.invalid("Curl URL is not valid.");
    }
    if (url.getScheme() == null
        || (!"http".equalsIgnoreCase(url.getScheme()) && !"https".equalsIgnoreCase(url.getScheme()))) {
      return CurlCommandParseResult.invalid("Only http and https curl targets are supported.");
    }

    String method =
        explicitMethod != null && !explicitMethod.isBlank()
            ? explicitMethod
            : body.isBlank() ? "GET" : "POST";
    return CurlCommandParseResult.success(
        new CurlExecutionRequest(normalized, source, url, method, followRedirects, headers, body));
  }

  private String normalizeContinuations(String rawText) {
    return rawText.replaceAll("\\\\\r?\n", " ");
  }

  private boolean containsUnsafeShellFeatures(String text) {
    return text.contains("|")
        || text.contains("$(")
        || text.contains("${")
        || text.contains("`")
        || text.contains("&&")
        || text.contains(";");
  }

  private int requireNextToken(int index, List<String> tokens, String option) {
    if (index + 1 >= tokens.size()) {
      throw new IllegalArgumentException("Missing value for " + option);
    }
    return index + 1;
  }

  private List<String> tokenize(String command) {
    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inSingle = false;
    boolean inDouble = false;
    boolean escaping = false;
    for (int index = 0; index < command.length(); index++) {
      char character = command.charAt(index);
      if (escaping) {
        current.append(character);
        escaping = false;
        continue;
      }
      if (character == '\\' && !inSingle) {
        escaping = true;
        continue;
      }
      if (character == '\'' && !inDouble) {
        inSingle = !inSingle;
        continue;
      }
      if (character == '"' && !inSingle) {
        inDouble = !inDouble;
        continue;
      }
      if (Character.isWhitespace(character) && !inSingle && !inDouble) {
        if (!current.isEmpty()) {
          tokens.add(current.toString());
          current.setLength(0);
        }
        continue;
      }
      current.append(character);
    }
    if (escaping || inSingle || inDouble) {
      throw new IllegalArgumentException("Curl command contains unterminated quoting.");
    }
    if (!current.isEmpty()) {
      tokens.add(current.toString());
    }
    return tokens;
  }
}
