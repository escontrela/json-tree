package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.model.JsonSemanticPath;
import com.davidpe.jsontree.application.model.JsonSemanticPathSegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JsonSemanticSearchPathResolverServiceTest {

  private final JsonSemanticSearchPathResolverService service =
      new JsonSemanticSearchPathResolverService(new ObjectMapper());

  @Test
  void resolvesRegexMatchesToPropertyAndValuePaths() {
    Set<JsonSemanticPath> paths =
        service.resolveMatchedPaths(
            """
            {
              "user": {
                "name": "David"
              },
              "roles": [
                { "name": "admin" },
                { "name": "editor" }
              ]
            }
            """,
            "name|admin");

    assertEquals(
        Set.of("user/name", "roles/[0]/name", "roles/[1]/name"),
        paths.stream().map(this::displayPath).collect(Collectors.toSet()));
  }

  @Test
  void supportsQuotedFieldNameRegexesUsingJsonSemantics() {
    Set<JsonSemanticPath> paths =
        service.resolveMatchedPaths(
            """
            {
              "api-response": {
                "status": "ok"
              }
            }
            """,
            "\"api-response\"");

    assertEquals(Set.of("api-response"), paths.stream().map(this::displayPath).collect(Collectors.toSet()));
  }

  private String displayPath(JsonSemanticPath path) {
    return path.segments().stream()
        .map(this::displaySegment)
        .collect(Collectors.joining("/"));
  }

  private String displaySegment(JsonSemanticPathSegment segment) {
    return segment.propertyName() != null
        ? segment.propertyName()
        : "[" + segment.arrayIndex() + "]";
  }
}
