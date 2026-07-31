package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonSemanticPath;
import com.davidpe.jsontree.application.model.JsonSemanticPathSegment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;

/**
 * Resolves an active regex query to semantic JSON paths instead of rendered text ranges.
 */
@Service
public class JsonSemanticSearchPathResolverService {

  private final ObjectMapper objectMapper;

  public JsonSemanticSearchPathResolverService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Set<JsonSemanticPath> resolveMatchedPaths(String rawJson, String regexQuery) {
    if (rawJson == null || rawJson.isBlank() || regexQuery == null || regexQuery.isBlank()) {
      return Set.of();
    }

    Pattern pattern;
    try {
      pattern = Pattern.compile(regexQuery);
    } catch (PatternSyntaxException exception) {
      throw new IllegalArgumentException("Invalid regular expression for crop.", exception);
    }

    try {
      JsonNode rootNode = objectMapper.readTree(rawJson);
      LinkedHashSet<JsonSemanticPath> resolvedPaths = new LinkedHashSet<>();
      collectMatches(rootNode, JsonSemanticPath.root(), pattern, resolvedPaths);
      return resolvedPaths;
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to resolve semantic crop paths from JSON.", exception);
    }
  }

  private void collectMatches(
      JsonNode node,
      JsonSemanticPath currentPath,
      Pattern pattern,
      Set<JsonSemanticPath> resolvedPaths) {
    if (node == null) {
      return;
    }

    if (node.isObject()) {
      for (Map.Entry<String, JsonNode> field : node.properties()) {
        JsonSemanticPath fieldPath =
            currentPath.append(JsonSemanticPathSegment.property(field.getKey()));
        if (matchesFieldName(field.getKey(), pattern)) {
          resolvedPaths.add(fieldPath);
        }
        collectMatches(field.getValue(), fieldPath, pattern, resolvedPaths);
      }
      return;
    }

    if (node.isArray()) {
      for (int index = 0; index < node.size(); index++) {
        collectMatches(
            node.get(index),
            currentPath.append(JsonSemanticPathSegment.arrayIndex(index)),
            pattern,
            resolvedPaths);
      }
      return;
    }

    if (pattern.matcher(node.toString()).find()) {
      resolvedPaths.add(currentPath);
    }
  }

  private boolean matchesFieldName(String fieldName, Pattern pattern) {
    return pattern.matcher(fieldName).find()
        || pattern.matcher(quotedFieldName(fieldName)).find();
  }

  private String quotedFieldName(String fieldName) {
    try {
      return objectMapper.writeValueAsString(fieldName);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to quote a JSON field name for crop.", exception);
    }
  }
}
