package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonOutlineEntry;
import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.application.model.LargePreviewOutlineDigest;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class JsonOutlineModelService {

  private static final Pattern ARRAY_LABEL_PATTERN = Pattern.compile("^(.+)\\s\\[(\\d+)]$");
  private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(.+?):\\s(.+)$");

  private final ObjectMapper objectMapper;

  public JsonOutlineModelService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public JsonOutlineModel buildFromRawJson(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return JsonOutlineModel.empty();
    }

    JsonNode rootNode;
    try {
      rootNode = objectMapper.readTree(rawJson);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to generate outline model from JSON.", exception);
    }

    List<JsonOutlineEntry> entries = new ArrayList<>();
    appendEntry(rootNode, 0, 8, entries);
    appendChildren(rootNode, 1, entries);

    int maxDepth =
        entries.stream()
            .mapToInt(JsonOutlineEntry::depth)
            .max()
            .orElse(0);

    return new JsonOutlineModel(entries, maxDepth);
  }

  public JsonOutlineModel buildFromAsciiPreview(AsciiTreeDocument document) {
    if (document == null || document.content().isBlank()) {
      return JsonOutlineModel.empty();
    }

    List<JsonOutlineEntry> entries = new ArrayList<>();
    String[] lines = document.content().split("\\R");
    for (String line : lines) {
      if (line == null || line.isBlank()) {
        continue;
      }
      entries.add(entryFromPreviewLine(line));
    }

    int maxDepth = entries.stream().mapToInt(JsonOutlineEntry::depth).max().orElse(0);
    return entries.isEmpty() ? JsonOutlineModel.empty() : new JsonOutlineModel(entries, maxDepth);
  }

  public JsonOutlineModel buildFromLargePreviewDigest(LargePreviewOutlineDigest digest) {
    if (digest == null || digest.emptyDigest()) {
      return JsonOutlineModel.empty();
    }
    return new JsonOutlineModel(
        digest.entries().stream().map(entry -> entry.outlineEntry()).toList(), digest.maxDepth());
  }

  private void appendChildren(JsonNode node, int depth, List<JsonOutlineEntry> entries) {
    if (node == null || !node.isContainerNode()) {
      return;
    }

    if (node.isObject()) {
      Iterator<String> fieldNames = node.fieldNames();
      while (fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        JsonNode child = node.get(fieldName);
        appendEntry(child, depth, fieldName.length(), entries);
        appendChildren(child, depth + 1, entries);
      }
      return;
    }

    if (node.isArray()) {
      for (JsonNode child : node) {
        appendEntry(child, depth, 6, entries);
        appendChildren(child, depth + 1, entries);
      }
    }
  }

  private void appendEntry(
      JsonNode node,
      int depth,
      int labelLength,
      List<JsonOutlineEntry> entries
  ) {
    JsonOutlineEntryKind kind = kindFor(node);
    int childCount = node != null && node.isContainerNode() ? node.size() : 0;
    int visualWeight = computeVisualWeight(node, labelLength, childCount, kind);
    entries.add(new JsonOutlineEntry(depth, visualWeight, kind, childCount));
  }

  private JsonOutlineEntry entryFromPreviewLine(String line) {
    int depth = previewDepth(line);
    String payload = previewPayload(line);
    JsonOutlineEntryKind kind = previewKind(payload);
    int childCount = previewChildCount(payload, kind);
    int visualWeight = computePreviewVisualWeight(payload, kind, childCount);
    return new JsonOutlineEntry(depth, visualWeight, kind, childCount);
  }

  private int previewDepth(String line) {
    int branchIndex = Math.max(line.lastIndexOf("├─ "), line.lastIndexOf("└─ "));
    if (branchIndex < 0) {
      return 0;
    }
    int depth = 1;
    for (int index = 0; index < branchIndex; index++) {
      if (line.charAt(index) == '│') {
        depth++;
      }
    }
    return depth;
  }

  private String previewPayload(String line) {
    int branchIndex = Math.max(line.lastIndexOf("├─ "), line.lastIndexOf("└─ "));
    return branchIndex < 0 ? line.trim() : line.substring(branchIndex + 3).trim();
  }

  private JsonOutlineEntryKind previewKind(String payload) {
    if (payload.startsWith("... ")) {
      return JsonOutlineEntryKind.VALUE;
    }
    Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(payload);
    if (keyValueMatcher.matches()) {
      String value = keyValueMatcher.group(2);
      if (value.startsWith("{")) {
        return JsonOutlineEntryKind.OBJECT;
      }
      if (value.startsWith("[")) {
        return JsonOutlineEntryKind.ARRAY;
      }
      return JsonOutlineEntryKind.VALUE;
    }
    if (ARRAY_LABEL_PATTERN.matcher(payload).matches()) {
      return JsonOutlineEntryKind.ARRAY;
    }
    return JsonOutlineEntryKind.OBJECT;
  }

  private int previewChildCount(String payload, JsonOutlineEntryKind kind) {
    Matcher arrayMatcher = ARRAY_LABEL_PATTERN.matcher(payload);
    if (arrayMatcher.matches()) {
      return Integer.parseInt(arrayMatcher.group(2));
    }
    if (kind == JsonOutlineEntryKind.ARRAY && payload.contains("[")) {
      return 1;
    }
    if (kind == JsonOutlineEntryKind.OBJECT && payload.contains("{")) {
      return 1;
    }
    return 0;
  }

  private int computePreviewVisualWeight(
      String payload, JsonOutlineEntryKind kind, int childCount) {
    int baseWeight =
        switch (kind) {
          case OBJECT -> 18;
          case ARRAY -> 16;
          case VALUE -> 10;
        };
    int signal = Math.min(Math.max(payload.length(), childCount), 10);
    return clamp(Math.max(baseWeight, signal + 8), 6, 30);
  }

  private JsonOutlineEntryKind kindFor(JsonNode node) {
    if (node != null && node.isObject()) {
      return JsonOutlineEntryKind.OBJECT;
    }
    if (node != null && node.isArray()) {
      return JsonOutlineEntryKind.ARRAY;
    }
    return JsonOutlineEntryKind.VALUE;
  }

  private int computeVisualWeight(
      JsonNode node,
      int labelLength,
      int childCount,
      JsonOutlineEntryKind kind
  ) {
    int baseWeight =
        switch (kind) {
          case OBJECT -> 18;
          case ARRAY -> 16;
          case VALUE -> 10;
        };
    int nodeSignal = node == null || node.isContainerNode()
        ? childCount
        : node.asText().length();
    return clamp(Math.max(baseWeight, labelLength + Math.min(nodeSignal, 10)), 6, 30);
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
