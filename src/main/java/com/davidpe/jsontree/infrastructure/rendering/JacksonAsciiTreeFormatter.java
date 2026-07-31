package com.davidpe.jsontree.infrastructure.rendering;

import com.davidpe.jsontree.application.port.out.AsciiTreeRendererPort;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JacksonAsciiTreeFormatter implements AsciiTreeRendererPort {

    private final ObjectMapper objectMapper;
    private final LargePreviewProperties largePreviewProperties;

    public JacksonAsciiTreeFormatter(ObjectMapper objectMapper) {
        this(objectMapper, new LargePreviewProperties());
    }

    @Autowired
    public JacksonAsciiTreeFormatter(
            ObjectMapper objectMapper,
            LargePreviewProperties largePreviewProperties) {
        this.objectMapper = objectMapper;
        this.largePreviewProperties = largePreviewProperties;
    }

    @Override
    public AsciiTreeDocument render(Path jsonFilePath) {
        try {
            return format(objectMapper.readTree(Files.readString(jsonFilePath)));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render ASCII tree from: " + jsonFilePath, exception);
        }
    }

    @Override
    public AsciiTreeDocument renderRawJson(String rawJson) {
        try {
            return format(objectMapper.readTree(rawJson));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render ASCII tree from in-memory JSON.", exception);
        }
    }

    @Override
    public AsciiTreeDocument renderLargePreview(Path jsonFilePath) {
        try (InputStream inputStream = Files.newInputStream(jsonFilePath);
             JsonParser parser = objectMapper.getFactory().createParser(inputStream)) {
            PreviewBuilder builder = new PreviewBuilder(largePreviewProperties.getPreviewMaxLines());
            builder.addRoot();

            JsonToken rootToken = parser.nextToken();
            if (rootToken == null) {
                return builder.build();
            }

            if (rootToken == JsonToken.START_OBJECT) {
                appendObjectPreview(builder, parser, "", 0);
            } else if (rootToken == JsonToken.START_ARRAY) {
                appendArrayPreview(builder, parser, "", 0);
            } else {
                builder.appendLine("├─ value: " + formatScalar(parser, rootToken));
            }

            return builder.build();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to render large-preview ASCII tree from: " + jsonFilePath, exception);
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

    private boolean appendObjectPreview(
            PreviewBuilder builder,
            JsonParser parser,
            String prefix,
            int depth) throws IOException {
        int childCount = 0;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            JsonToken valueToken = parser.nextToken();

            if (childCount >= largePreviewProperties.getPreviewMaxChildrenPerContainer()) {
                builder.appendLine(prefix + "├─ ... object entries truncated after "
                        + largePreviewProperties.getPreviewMaxChildrenPerContainer() + " fields");
                skipRemainingObject(parser);
                return false;
            }

            if (!appendPreviewEntry(builder, parser, prefix, depth, fieldName, valueToken)) {
                skipRemainingObject(parser);
                return false;
            }
            childCount++;
            if (builder.truncated()) {
                skipRemainingObject(parser);
                return false;
            }
        }
        return true;
    }

    private boolean appendArrayPreview(
            PreviewBuilder builder,
            JsonParser parser,
            String prefix,
            int depth) throws IOException {
        int childCount = 0;
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            JsonToken valueToken = parser.currentToken();
            String label = "[" + childCount + "]";

            if (childCount >= largePreviewProperties.getPreviewMaxChildrenPerContainer()) {
                builder.appendLine(prefix + "├─ ... array entries truncated after "
                        + largePreviewProperties.getPreviewMaxChildrenPerContainer() + " items");
                skipRemainingArray(parser);
                return false;
            }

            if (!appendPreviewEntry(builder, parser, prefix, depth, label, valueToken)) {
                skipRemainingArray(parser);
                return false;
            }
            childCount++;
            if (builder.truncated()) {
                skipRemainingArray(parser);
                return false;
            }
        }
        return true;
    }

    private boolean appendPreviewEntry(
            PreviewBuilder builder,
            JsonParser parser,
            String prefix,
            int depth,
            String label,
            JsonToken valueToken) throws IOException {
        if (valueToken == JsonToken.START_OBJECT) {
            if (depth >= largePreviewProperties.getPreviewMaxDepth()) {
                builder.appendLine(prefix + "├─ " + label + " {... depth limit}");
                parser.skipChildren();
                return !builder.truncated();
            }
            if (!builder.appendLine(prefix + "├─ " + label)) {
                builder.appendOverflowNotice(prefix);
                parser.skipChildren();
                return false;
            }
            return appendObjectPreview(builder, parser, prefix + "│  ", depth + 1);
        }

        if (valueToken == JsonToken.START_ARRAY) {
            if (depth >= largePreviewProperties.getPreviewMaxDepth()) {
                builder.appendLine(prefix + "├─ " + label + " [... depth limit]");
                parser.skipChildren();
                return !builder.truncated();
            }
            if (!builder.appendLine(prefix + "├─ " + label + " [preview]")) {
                builder.appendOverflowNotice(prefix);
                parser.skipChildren();
                return false;
            }
            return appendArrayPreview(builder, parser, prefix + "│  ", depth + 1);
        }

        if (!builder.appendLine(prefix + "├─ " + label + ": " + formatScalar(parser, valueToken))) {
            builder.appendOverflowNotice(prefix);
            return false;
        }
        return true;
    }

    private void skipRemainingObject(JsonParser parser) throws IOException {
        parser.skipChildren();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() == JsonToken.FIELD_NAME) {
                parser.nextToken();
                parser.skipChildren();
            }
        }
    }

    private void skipRemainingArray(JsonParser parser) throws IOException {
        parser.skipChildren();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            parser.skipChildren();
        }
    }

    private String formatScalar(JsonParser parser, JsonToken token) throws IOException {
        return switch (token) {
            case VALUE_STRING -> "\"" + parser.getText() + "\"";
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT, VALUE_TRUE, VALUE_FALSE, VALUE_NULL ->
                    parser.getText();
            default -> "\"<unsupported>\"";
        };
    }

    private static final class PreviewBuilder {

        private final int maxLines;
        private final List<String> lines = new ArrayList<>();
        private boolean truncated;

        private PreviewBuilder(int maxLines) {
            this.maxLines = Math.max(1, maxLines);
        }

        private void addRoot() {
            lines.add("root");
        }

        private boolean appendLine(String line) {
            if (truncated) {
                return false;
            }
            if (lines.size() < maxLines) {
                lines.add(line);
                return true;
            }
            return false;
        }

        private void appendOverflowNotice(String prefix) {
            if (truncated) {
                return;
            }
            truncated = true;
            String notice = prefix + "├─ ... preview truncated after " + maxLines + " lines";
            if (lines.size() >= maxLines) {
                lines.set(maxLines - 1, notice);
                return;
            }
            lines.add(notice);
        }

        private boolean truncated() {
            return truncated;
        }

        private AsciiTreeDocument build() {
            String content = String.join("\n", lines);
            return new AsciiTreeDocument("root", content, lines.size());
        }
    }
}
