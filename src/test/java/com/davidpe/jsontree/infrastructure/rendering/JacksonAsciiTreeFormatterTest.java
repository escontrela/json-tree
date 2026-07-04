package com.davidpe.jsontree.infrastructure.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JacksonAsciiTreeFormatterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JacksonAsciiTreeFormatter formatter = new JacksonAsciiTreeFormatter(objectMapper);

    @TempDir
    Path tempDir;

    @Test
    void rendersObjectsArraysAndPrimitiveValuesWithStableIndentation() throws Exception {
        AsciiTreeDocument document = formatter.format(objectMapper.readTree("""
                {
                  "user": {
                    "id": 12345,
                    "name": "David",
                    "active": true
                  },
                  "tags": ["dev", "json"]
                }
                """));

        assertEquals("root", document.rootLabel());
        assertEquals("""
                root
                ├─ user
                │  ├─ id: 12345
                │  ├─ name: "David"
                │  └─ active: true
                └─ tags [2]
                   ├─ [0]: "dev"
                   └─ [1]: "json\"""".stripTrailing(), document.content());
        assertEquals(8, document.lineCount());
    }

    @Test
    void keepsDeepNestingAlignedAcrossMixedContainers() throws Exception {
        AsciiTreeDocument document = formatter.format(objectMapper.readTree("""
                {
                  "config": {
                    "profiles": [
                      {
                        "name": "prod",
                        "flags": [true, false]
                      }
                    ]
                  }
                }
                """));

        assertEquals("""
                root
                └─ config
                   └─ profiles [1]
                      └─ [0]
                         ├─ name: "prod"
                         └─ flags [2]
                            ├─ [0]: true
                            └─ [1]: false""", document.content());
        assertEquals(8, document.lineCount());
    }

    @Test
    void rendersLargePreviewWithoutBuildingFullTreeState() throws Exception {
        Path jsonFile = Files.writeString(
                tempDir.resolve("large-preview.json"),
                """
                {
                  "user": {
                    "id": 12345,
                    "name": "David",
                    "roles": ["developer", "designer", "admin"]
                  },
                  "flags": {
                    "active": true,
                    "beta": false
                  }
                }
                """);
        JacksonAsciiTreeFormatter previewFormatter = formatterWithBudgets(20, 4, 4);

        AsciiTreeDocument document = previewFormatter.renderLargePreview(jsonFile);

        assertEquals("""
                root
                ├─ user
                │  ├─ id: 12345
                │  ├─ name: "David"
                │  ├─ roles [preview]
                │  │  ├─ [0]: "developer"
                │  │  ├─ [1]: "designer"
                │  │  ├─ [2]: "admin"
                ├─ flags
                │  ├─ active: true
                │  ├─ beta: false""", document.content());
        assertEquals(11, document.lineCount());
    }

    @Test
    void appliesDepthAndSiblingCapsWithExplicitTruncationLines() throws Exception {
        Path jsonFile = Files.writeString(
                tempDir.resolve("capped-preview.json"),
                """
                {
                  "items": [
                    {"id": 1, "meta": {"deep": {"value": "a"}}},
                    {"id": 2},
                    {"id": 3}
                  ],
                  "extra": 99
                }
                """);
        JacksonAsciiTreeFormatter previewFormatter = formatterWithBudgets(20, 2, 2);

        AsciiTreeDocument document = previewFormatter.renderLargePreview(jsonFile);

        assertTrue(document.content().contains("meta {... depth limit}"));
        assertTrue(document.content().contains("... array entries truncated after 2 items"));
    }

    @Test
    void capsPreviewLinesWithStableFinalNotice() throws Exception {
        Path jsonFile = Files.writeString(
                tempDir.resolve("line-budget-preview.json"),
                """
                {
                  "a": 1,
                  "b": 2,
                  "c": 3,
                  "d": 4,
                  "e": 5,
                  "f": 6
                }
                """);
        JacksonAsciiTreeFormatter previewFormatter = formatterWithBudgets(5, 4, 8);

        AsciiTreeDocument document = previewFormatter.renderLargePreview(jsonFile);

        assertEquals(5, document.lineCount());
        assertTrue(document.content().endsWith("├─ ... preview truncated after 5 lines"));
    }

    private JacksonAsciiTreeFormatter formatterWithBudgets(
            int previewMaxLines,
            int previewMaxDepth,
            int previewMaxChildrenPerContainer) {
        LargePreviewProperties properties = new LargePreviewProperties();
        properties.setPreviewMaxLines(previewMaxLines);
        properties.setPreviewMaxDepth(previewMaxDepth);
        properties.setPreviewMaxChildrenPerContainer(previewMaxChildrenPerContainer);
        return new JacksonAsciiTreeFormatter(objectMapper, properties);
    }
}
