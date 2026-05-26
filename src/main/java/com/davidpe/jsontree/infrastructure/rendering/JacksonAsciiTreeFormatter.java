package com.davidpe.jsontree.infrastructure.rendering;

import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JacksonAsciiTreeFormatter implements AsciiTreeRendererPort {

    private final ObjectMapper objectMapper;

    public JacksonAsciiTreeFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AsciiTreeDocument render(Path jsonFilePath) {
        try {
            return format(objectMapper.readTree(Files.readString(jsonFilePath)));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render ASCII tree from: " + jsonFilePath, exception);
        }
    }

    public AsciiTreeDocument format(JsonNode rootNode) {
        StringBuilder content = new StringBuilder("root");
        appendNodeChildren(content, rootNode, "");
        String renderedContent = content.toString();
        int lineCount = renderedContent.isEmpty() ? 0 : renderedContent.split("\\R", -1).length;
        return new AsciiTreeDocument("root", renderedContent, lineCount);
    }

    private void appendNodeChildren(StringBuilder content, JsonNode node, String prefix) {
        if (node.isObject()) {
            appendObjectChildren(content, node, prefix);
            return;
        }
        if (node.isArray()) {
            appendArrayChildren(content, node, prefix);
            return;
        }
        content.append('\n').append(prefix).append(formatPrimitive(node));
    }

    private void appendObjectChildren(StringBuilder content, JsonNode objectNode, String prefix) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            boolean last = !fields.hasNext();
            appendBranch(content, prefix, last, field.getKey(), field.getValue());
        }
    }

    private void appendArrayChildren(StringBuilder content, JsonNode arrayNode, String prefix) {
        for (int index = 0; index < arrayNode.size(); index++) {
            boolean last = index == arrayNode.size() - 1;
            appendBranch(content, prefix, last, "[" + index + "]", arrayNode.get(index));
        }
    }

    private void appendBranch(StringBuilder content, String prefix, boolean last, String label, JsonNode childNode) {
        content.append('\n')
                .append(prefix)
                .append(last ? "└─ " : "├─ ")
                .append(formatLabel(label, childNode));

        if (childNode.isContainerNode()) {
            String childPrefix = prefix + (last ? "   " : "│  ");
            appendNodeChildren(content, childNode, childPrefix);
        }
    }

    private String formatLabel(String label, JsonNode node) {
        if (node.isArray()) {
            return label + " [" + node.size() + "]";
        }
        if (node.isObject()) {
            return label;
        }
        return label + ": " + formatPrimitive(node);
    }

    private String formatPrimitive(JsonNode node) {
        return node.toString();
    }
}
