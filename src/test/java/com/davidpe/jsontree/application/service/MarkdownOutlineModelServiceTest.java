package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.JsonOutlineEntryKind;
import com.davidpe.jsontree.application.model.JsonOutlineModel;
import com.davidpe.jsontree.application.model.MarkdownOutlineEntry;
import com.davidpe.jsontree.application.model.MarkdownOutlineModel;
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

  @Test
  void minimapModelHasOneEntryPerSourceLineSoRowsStayProportionalToRealPosition() {
    var model =
        service.build(
            """
            # Root

            filler
            filler
            filler
            filler
            filler
            filler
            filler
            filler

            ## Near the end
            """);

    JsonOutlineModel minimapModel = service.toMinimapModel(model);

    assertEquals(model.totalLines(), minimapModel.entries().size());
    assertEquals(JsonOutlineEntryKind.OBJECT, minimapModel.entries().get(0).kind());
    int secondHeadingLine = model.entries().get(1).sourceLineIndex();
    assertEquals(JsonOutlineEntryKind.OBJECT, minimapModel.entries().get(secondHeadingLine).kind());
    // A filler line between the two headings must not be painted as a heading anchor.
    assertEquals(JsonOutlineEntryKind.VALUE, minimapModel.entries().get(2).kind());
  }

  @Test
  void anchorLineForPointerReturnsMidpointOfTheLineRangeDirectly() {
    var model =
        new MarkdownOutlineModel(
            java.util.List.of(new MarkdownOutlineEntry("Root", 0, 0, 10, false)), 0, true, 100);

    assertEquals(24, service.anchorLineForPointer(model, 20, 30));
    assertEquals(0, service.anchorLineForPointer(model, -5, 1));
    assertEquals(97, service.anchorLineForPointer(model, 95, 500));
  }
}
