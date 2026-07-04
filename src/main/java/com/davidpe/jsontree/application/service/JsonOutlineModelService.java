package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonOutlineEntry;
import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JsonOutlineModelService {

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
