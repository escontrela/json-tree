package com.davidpe.jsontree.infrastructure.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JacksonAsciiTreeFormatterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JacksonAsciiTreeFormatter formatter = new JacksonAsciiTreeFormatter();

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
}
