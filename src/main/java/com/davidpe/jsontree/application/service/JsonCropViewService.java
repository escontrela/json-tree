package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonCropDocument;
import com.davidpe.jsontree.application.model.JsonSemanticPath;
import com.davidpe.jsontree.application.model.JsonSemanticPathSegment;
import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Builds an ephemeral cropped JSON document by preserving only the branches matched by a regex.
 */
@Service
public class JsonCropViewService {

  private final ObjectMapper objectMapper;
  private final JsonSemanticSearchPathResolverService semanticPathResolverService;
  private final AsciiTreeRendererPort asciiTreeRendererPort;

  public JsonCropViewService(
      ObjectMapper objectMapper,
      JsonSemanticSearchPathResolverService semanticPathResolverService,
      AsciiTreeRendererPort asciiTreeRendererPort) {
    this.objectMapper = objectMapper;
    this.semanticPathResolverService = semanticPathResolverService;
    this.asciiTreeRendererPort = asciiTreeRendererPort;
  }

  public Optional<JsonCropDocument> buildFromQuery(String rawJson, String regexQuery) {
    if (rawJson == null || rawJson.isBlank() || regexQuery == null || regexQuery.isBlank()) {
      return Optional.empty();
    }

    Set<JsonSemanticPath> matchedPaths =
        semanticPathResolverService.resolveMatchedPaths(rawJson, regexQuery);
    if (matchedPaths.isEmpty()) {
      return Optional.empty();
    }

    try {
      JsonNode sourceRoot = objectMapper.readTree(rawJson);
      JsonNode croppedRoot = cropNode(sourceRoot, List.copyOf(matchedPaths), 0);
      if (croppedRoot == null) {
        return Optional.empty();
      }
      String croppedRawJson = objectMapper.writeValueAsString(croppedRoot);
      return Optional.of(
          new JsonCropDocument(croppedRawJson, asciiTreeRendererPort.renderRawJson(croppedRawJson)));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to build cropped JSON view.", exception);
    }
  }

  private JsonNode cropNode(JsonNode sourceNode, List<JsonSemanticPath> paths, int depth) {
    if (sourceNode == null || paths.isEmpty()) {
      return null;
    }

    if (paths.stream().anyMatch(path -> path.segments().size() == depth)) {
      return sourceNode.deepCopy();
    }

    if (sourceNode.isObject()) {
      return cropObjectNode(sourceNode, paths, depth);
    }
    if (sourceNode.isArray()) {
      return cropArrayNode(sourceNode, paths, depth);
    }
    return null;
  }

  private JsonNode cropObjectNode(JsonNode sourceNode, List<JsonSemanticPath> paths, int depth) {
    ObjectNode croppedNode = objectMapper.createObjectNode();
    Map<String, List<JsonSemanticPath>> pathsByField = new LinkedHashMap<>();
    for (JsonSemanticPath path : paths) {
      JsonSemanticPathSegment segment = path.segments().get(depth);
      if (segment.propertySegment()) {
        pathsByField.computeIfAbsent(segment.propertyName(), unused -> new java.util.ArrayList<>())
            .add(path);
      }
    }

    for (Map.Entry<String, JsonNode> field : sourceNode.properties()) {
      List<JsonSemanticPath> childPaths = pathsByField.get(field.getKey());
      if (childPaths == null || childPaths.isEmpty()) {
        continue;
      }
      JsonNode croppedChild = cropNode(field.getValue(), childPaths, depth + 1);
      if (croppedChild != null) {
        croppedNode.set(field.getKey(), croppedChild);
      }
    }

    return croppedNode.isEmpty() ? null : croppedNode;
  }

  private JsonNode cropArrayNode(JsonNode sourceNode, List<JsonSemanticPath> paths, int depth) {
    ArrayNode croppedNode = objectMapper.createArrayNode();
    Map<Integer, List<JsonSemanticPath>> pathsByIndex = new LinkedHashMap<>();
    for (JsonSemanticPath path : paths) {
      JsonSemanticPathSegment segment = path.segments().get(depth);
      if (segment.arrayIndexSegment()) {
        pathsByIndex.computeIfAbsent(segment.arrayIndex(), unused -> new java.util.ArrayList<>())
            .add(path);
      }
    }

    for (int index = 0; index < sourceNode.size(); index++) {
      List<JsonSemanticPath> childPaths = pathsByIndex.get(index);
      if (childPaths == null || childPaths.isEmpty()) {
        continue;
      }
      JsonNode croppedChild = cropNode(sourceNode.get(index), childPaths, depth + 1);
      if (croppedChild != null) {
        croppedNode.add(croppedChild);
      }
    }

    return croppedNode.isEmpty() ? null : croppedNode;
  }
}
