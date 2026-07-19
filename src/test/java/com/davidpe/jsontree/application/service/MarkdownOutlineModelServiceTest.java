package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownOutlineModelServiceTest {

  private final MarkdownOutlineModelService service = new MarkdownOutlineModelService();

  @Test
  void buildsHeadingDrivenOutlineIgnoringFencedCodeHeadings() {
    var model =
        service.build(
            """
            # Root

            ## Section
            ```
            # not-a-heading
            ```
            ### Detail
            """);

    assertTrue(model.headingDriven());
    assertEquals(3, model.entries().size());
    assertEquals("Root", model.entries().get(0).title());
    assertEquals("Section", model.entries().get(1).title());
    assertEquals("Detail", model.entries().get(2).title());
    assertEquals(2, model.maxDepth());
  }

  @Test
  void fallsBackToSourceAnchorsWhenDocumentHasNoHeadings() {
    var model =
        service.build(
            """
            first line
            second line

            third line
            fourth line
            """);

    assertFalse(model.headingDriven());
    assertFalse(model.emptyModel());
    assertTrue(model.entries().stream().allMatch(entry -> entry.fallback()));
    assertEquals(0, model.entries().getFirst().sourceLineIndex());
  }
}
