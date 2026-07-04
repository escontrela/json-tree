package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonOutlineModelServiceTest {

  private final JsonOutlineModelService service = new JsonOutlineModelService(new ObjectMapper());

  @Test
  void buildsCompactHierarchyForNestedObjectAndArrayStructures() {
    JsonOutlineModel model =
        service.buildFromRawJson(
            """
            {
              "user": {
                "id": 42,
                "roles": ["admin", "editor"]
              },
              "active": true
            }
            """);

    assertFalse(model.emptyModel());
    assertEquals(7, model.totalEntries());
    assertEquals(3, model.maxDepth());
    assertEquals(JsonOutlineEntryKind.OBJECT, model.entries().get(0).kind());
    assertEquals(JsonOutlineEntryKind.OBJECT, model.entries().get(1).kind());
    assertEquals(JsonOutlineEntryKind.VALUE, model.entries().get(2).kind());
    assertEquals(JsonOutlineEntryKind.ARRAY, model.entries().get(3).kind());
  }

  @Test
  void buildsOutlineForRootArrayDocuments() {
    JsonOutlineModel model =
        service.buildFromRawJson(
            """
            [
              {"name": "alpha"},
              {"name": "beta"}
            ]
            """);

    assertEquals(JsonOutlineEntryKind.ARRAY, model.entries().getFirst().kind());
    assertTrue(model.totalEntries() >= 5);
    assertTrue(model.maxDepth() >= 2);
  }

  @Test
  void returnsEmptyModelForBlankInput() {
    JsonOutlineModel model = service.buildFromRawJson("   ");

    assertTrue(model.emptyModel());
    assertEquals(0, model.totalEntries());
  }

  @Test
  void rejectsInvalidJsonPayloads() {
    assertThrows(IllegalStateException.class, () -> service.buildFromRawJson("{invalid"));
  }

  @Test
  void buildsOutlineFromBoundedAsciiPreview() {
    JsonOutlineModel model =
        service.buildFromAsciiPreview(
            new AsciiTreeDocument(
                "root",
                """
                root { 3 keys
                ├─ app { 2 keys
                │  ├─ name : "json-tree"
                │  └─ items [3]
                ├─ ... object entries truncated after 64 fields
                └─ user : {... depth limit}
                """,
                6));

    assertFalse(model.emptyModel());
    assertEquals(6, model.totalEntries());
    assertEquals(2, model.maxDepth());
    assertEquals(JsonOutlineEntryKind.OBJECT, model.entries().getFirst().kind());
    assertEquals(JsonOutlineEntryKind.ARRAY, model.entries().get(3).kind());
    assertEquals(JsonOutlineEntryKind.VALUE, model.entries().get(4).kind());
    assertEquals(JsonOutlineEntryKind.OBJECT, model.entries().get(5).kind());
  }
}
