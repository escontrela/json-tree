package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Generates a structure-only ASCII tree that preserves property names while omitting real values.
 */
@Service
public class JsonStructureDocumentService {

  private final ObjectMapper objectMapper;

  public JsonStructureDocumentService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AsciiTreeDocument buildFromRawJson(String rawJson) {
    try {
      return format(objectMapper.readTree(rawJson));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to generate structure tree from JSON.", exception);
    }
  }

  AsciiTreeDocument format(JsonNode rootNode) {
    StructureShape rootShape = toShape(rootNode);
    StringBuilder content = new StringBuilder(rootShape.kind() == StructureKind.ARRAY ? "root []" : "root");
    appendChildren(content, rootShape, "");
    String renderedContent = content.toString();
    int lineCount = renderedContent.isEmpty() ? 0 : renderedContent.split("\\R", -1).length;
    return new AsciiTreeDocument("root", renderedContent, lineCount);
  }

  private void appendChildren(StringBuilder content, StructureShape shape, String prefix) {
    if (shape.kind() == StructureKind.OBJECT) {
      appendObjectChildren(content, shape, prefix);
      return;
    }
    if (shape.kind() == StructureKind.ARRAY && shape.arrayItemShape() != null) {
      appendBranch(content, prefix, true, "[0]", shape.arrayItemShape());
    }
  }

  private void appendObjectChildren(StringBuilder content, StructureShape objectShape, String prefix) {
    List<Map.Entry<String, StructureShape>> fields = new ArrayList<>(objectShape.objectFields().entrySet());
    for (int index = 0; index < fields.size(); index++) {
      Map.Entry<String, StructureShape> field = fields.get(index);
      appendBranch(content, prefix, index == fields.size() - 1, field.getKey(), field.getValue());
    }
  }

  private void appendBranch(
      StringBuilder content, String prefix, boolean last, String label, StructureShape shape) {
    content.append('\n').append(prefix).append(last ? "└─ " : "├─ ").append(formatLabel(label, shape));
    if (!shape.hasChildren()) {
      return;
    }
    appendChildren(content, shape, prefix + (last ? "   " : "│  "));
  }

  private String formatLabel(String label, StructureShape shape) {
    return shape.kind() == StructureKind.ARRAY ? label + " []" : label;
  }

  private StructureShape toShape(JsonNode node) {
    if (node == null || node.isNull() || node.isValueNode()) {
      return StructureShape.scalar();
    }
    if (node.isObject()) {
      LinkedHashMap<String, StructureShape> fields = new LinkedHashMap<>();
      Iterator<Map.Entry<String, JsonNode>> iterator = node.properties().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, JsonNode> field = iterator.next();
        fields.put(field.getKey(), toShape(field.getValue()));
      }
      return StructureShape.object(fields);
    }
    if (node.isArray()) {
      if (node.isEmpty()) {
        return StructureShape.emptyArray();
      }
      return StructureShape.array(toShape(node.get(0)));
    }
    return StructureShape.scalar();
  }

  private enum StructureKind {
    OBJECT,
    ARRAY,
    SCALAR
  }

  private record StructureShape(
      StructureKind kind,
      LinkedHashMap<String, StructureShape> objectFields,
      StructureShape arrayItemShape) {

    static StructureShape scalar() {
      return new StructureShape(StructureKind.SCALAR, new LinkedHashMap<>(), null);
    }

    static StructureShape object(LinkedHashMap<String, StructureShape> objectFields) {
      return new StructureShape(StructureKind.OBJECT, new LinkedHashMap<>(objectFields), null);
    }

    static StructureShape emptyArray() {
      return new StructureShape(StructureKind.ARRAY, new LinkedHashMap<>(), null);
    }

    static StructureShape array(StructureShape itemShape) {
      return new StructureShape(StructureKind.ARRAY, new LinkedHashMap<>(), itemShape);
    }

    boolean hasChildren() {
      return switch (kind) {
        case OBJECT -> !objectFields.isEmpty();
        case ARRAY -> arrayItemShape != null;
        case SCALAR -> false;
      };
    }
  }
}
