package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.JsonBreadcrumbAnchor;
import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonBreadcrumbPath;
import com.davidpe.jsontree.application.model.RawJsonPresentation;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JsonBreadcrumbModelService {

  private final ObjectMapper objectMapper;
  private final RawJsonPresentationService rawJsonPresentationService;

  public JsonBreadcrumbModelService(
      ObjectMapper objectMapper, RawJsonPresentationService rawJsonPresentationService) {
    this.objectMapper = objectMapper;
    this.rawJsonPresentationService = rawJsonPresentationService;
  }

  public JsonBreadcrumbModel buildFromRawJson(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return JsonBreadcrumbModel.unavailable();
    }

    JsonNode rootNode;
    try {
      rootNode = objectMapper.readTree(rawJson);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to generate breadcrumb model from JSON.", exception);
    }

    RawJsonPresentation rawPresentation = rawJsonPresentationService.present(rawJson);
    LinkedHashMap<JsonBreadcrumbPath, Integer> asciiLineAnchors = new LinkedHashMap<>();
    buildAsciiLineAnchors(rootNode, JsonBreadcrumbPath.root(), asciiLineAnchors, new int[] {0});

    LinkedHashMap<JsonBreadcrumbPath, Integer> rawLineAnchors =
        buildRawLineAnchors(rawJson, rawPresentation);

    List<JsonBreadcrumbAnchor> anchors = new ArrayList<>();
    for (Map.Entry<JsonBreadcrumbPath, Integer> entry : asciiLineAnchors.entrySet()) {
      int rawLineIndex = rawLineAnchors.getOrDefault(entry.getKey(), 0);
      anchors.add(new JsonBreadcrumbAnchor(entry.getKey(), entry.getValue(), rawLineIndex));
    }
    return new JsonBreadcrumbModel(true, anchors);
  }

  private void buildAsciiLineAnchors(
      JsonNode node,
      JsonBreadcrumbPath path,
      LinkedHashMap<JsonBreadcrumbPath, Integer> anchors,
      int[] lineCounter) {
    anchors.putIfAbsent(path, lineCounter[0]);
    if (node == null || !node.isContainerNode()) {
      return;
    }

    if (node.isObject()) {
      node.properties()
          .forEach(
              entry -> {
                lineCounter[0]++;
                JsonBreadcrumbPath childPath = path.appendProperty(entry.getKey());
                buildAsciiLineAnchors(entry.getValue(), childPath, anchors, lineCounter);
              });
      return;
    }

    for (int index = 0; index < node.size(); index++) {
      lineCounter[0]++;
      JsonBreadcrumbPath childPath = path.appendArrayIndex(index);
      buildAsciiLineAnchors(node.get(index), childPath, anchors, lineCounter);
    }
  }

  private LinkedHashMap<JsonBreadcrumbPath, Integer> buildRawLineAnchors(
      String rawJson, RawJsonPresentation rawPresentation) {
    LinkedHashMap<JsonBreadcrumbPath, Integer> anchors = new LinkedHashMap<>();
    JsonBreadcrumbPath rootPath = JsonBreadcrumbPath.root();
    anchors.put(rootPath, 0);
    int[] lineStartOffsets = lineStartOffsets(rawPresentation.content());

    try (JsonParser parser = objectMapper.getFactory().createParser(rawJson)) {
      ArrayDeque<RawBreadcrumbContext> stack = new ArrayDeque<>();
      JsonToken token;
      while ((token = parser.nextToken()) != null) {
        switch (token) {
          case START_OBJECT -> handleContainerStart(
              parser.currentTokenLocation(),
              stack,
              anchors,
              rawPresentation.sourceToDisplayBoundaries(),
              lineStartOffsets,
              true);
          case START_ARRAY -> handleContainerStart(
              parser.currentTokenLocation(),
              stack,
              anchors,
              rawPresentation.sourceToDisplayBoundaries(),
              lineStartOffsets,
              false);
          case END_OBJECT, END_ARRAY -> {
            if (!stack.isEmpty()) {
              stack.pop();
            }
          }
          case FIELD_NAME -> {
            if (!stack.isEmpty()) {
              RawBreadcrumbContext context = stack.peek();
              JsonBreadcrumbPath fieldPath = context.path().appendProperty(parser.getCurrentName());
              anchors.putIfAbsent(
                  fieldPath,
                  displayLineIndex(
                      toDisplayOffset(
                          parser.currentTokenLocation(), rawPresentation.sourceToDisplayBoundaries()),
                      lineStartOffsets));
              context.pendingPropertyPath = fieldPath;
            }
          }
          default -> handleScalarValue(
              parser.currentTokenLocation(),
              stack,
              anchors,
              rawPresentation.sourceToDisplayBoundaries(),
              lineStartOffsets);
        }
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to generate raw breadcrumb anchors.", exception);
    }

    return anchors;
  }

  private void handleContainerStart(
      JsonLocation tokenLocation,
      ArrayDeque<RawBreadcrumbContext> stack,
      LinkedHashMap<JsonBreadcrumbPath, Integer> anchors,
      int[] sourceToDisplayBoundaries,
      int[] lineStartOffsets,
      boolean objectContext) {
    JsonBreadcrumbPath path;
    if (stack.isEmpty()) {
      stack.push(new RawBreadcrumbContext(JsonBreadcrumbPath.root(), objectContext));
      return;
    }

    RawBreadcrumbContext parentContext = stack.peek();
    if (parentContext.arrayContext()) {
      path = parentContext.nextArrayPath();
      anchors.putIfAbsent(
          path, displayLineIndex(toDisplayOffset(tokenLocation, sourceToDisplayBoundaries), lineStartOffsets));
      parentContext.advanceArrayIndex();
    } else {
      path = parentContext.consumePendingPropertyPath();
      if (path == null) {
        path = parentContext.path();
      }
    }
    stack.push(new RawBreadcrumbContext(path, objectContext));
  }

  private void handleScalarValue(
      JsonLocation tokenLocation,
      ArrayDeque<RawBreadcrumbContext> stack,
      LinkedHashMap<JsonBreadcrumbPath, Integer> anchors,
      int[] sourceToDisplayBoundaries,
      int[] lineStartOffsets) {
    if (stack.isEmpty()) {
      return;
    }

    RawBreadcrumbContext context = stack.peek();
    if (context.arrayContext()) {
      JsonBreadcrumbPath path = context.nextArrayPath();
      anchors.putIfAbsent(
          path, displayLineIndex(toDisplayOffset(tokenLocation, sourceToDisplayBoundaries), lineStartOffsets));
      context.advanceArrayIndex();
      return;
    }
    context.pendingPropertyPath = null;
  }

  private int[] lineStartOffsets(String content) {
    ArrayList<Integer> starts = new ArrayList<>();
    starts.add(0);
    for (int index = 0; index < content.length(); index++) {
      if (content.charAt(index) == '\n' && index + 1 <= content.length()) {
        starts.add(index + 1);
      }
    }
    return starts.stream().mapToInt(Integer::intValue).toArray();
  }

  private int toDisplayOffset(JsonLocation tokenLocation, int[] sourceToDisplayBoundaries) {
    if (tokenLocation == null) {
      return 0;
    }
    long charOffset = Math.max(0L, tokenLocation.getCharOffset());
    int sourceOffset = (int) Math.min(charOffset, sourceToDisplayBoundaries.length - 1L);
    return sourceToDisplayBoundaries[sourceOffset];
  }

  private int displayLineIndex(int displayOffset, int[] lineStartOffsets) {
    int low = 0;
    int high = lineStartOffsets.length - 1;
    int resolved = 0;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      int value = lineStartOffsets[middle];
      if (value <= displayOffset) {
        resolved = middle;
        low = middle + 1;
      } else {
        high = middle - 1;
      }
    }
    return resolved;
  }

  private static final class RawBreadcrumbContext {

    private final JsonBreadcrumbPath path;
    private final boolean objectContext;
    private int nextArrayIndex;
    private JsonBreadcrumbPath pendingPropertyPath;

    private RawBreadcrumbContext(JsonBreadcrumbPath path, boolean objectContext) {
      this.path = path;
      this.objectContext = objectContext;
    }

    private JsonBreadcrumbPath path() {
      return path;
    }

    private boolean arrayContext() {
      return !objectContext;
    }

    private JsonBreadcrumbPath nextArrayPath() {
      return path.appendArrayIndex(nextArrayIndex);
    }

    private void advanceArrayIndex() {
      nextArrayIndex++;
    }

    private JsonBreadcrumbPath consumePendingPropertyPath() {
      JsonBreadcrumbPath pathToUse = pendingPropertyPath;
      pendingPropertyPath = null;
      return pathToUse;
    }
  }
}
