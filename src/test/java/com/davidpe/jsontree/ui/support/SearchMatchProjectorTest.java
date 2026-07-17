package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.model.JsonSearchMatch;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import com.davidpe.jsontree.application.model.RawJsonPresentation;
import com.davidpe.jsontree.application.service.BestEffortJsonPrettyPrinter;
import com.davidpe.jsontree.application.service.RawJsonPresentationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchMatchProjectorTest {

  private final SearchMatchProjector projector = new SearchMatchProjector();
  private final RawJsonPresentationService rawJsonPresentationService =
      new RawJsonPresentationService(new ObjectMapper(), new BestEffortJsonPrettyPrinter());

  @Test
  void projectsRawRangesFromSearchSessionIndexes() {
    JsonSearchSession session =
        new JsonSearchSession(
            "file:/sample.json",
            "David|admin",
            List.of(new JsonSearchMatch(9, 14, "David"), new JsonSearchMatch(26, 31, "admin")),
            1);

    List<SearchHighlightRange> ranges = projector.rawRanges(session);

    assertEquals(2, ranges.size());
    assertEquals(new SearchHighlightRange(9, 14, false), ranges.get(0));
    assertEquals(new SearchHighlightRange(26, 31, true), ranges.get(1));
  }

  @Test
  void projectsAsciiRangesByMatchOrder() {
    JsonSearchSession session =
        new JsonSearchSession(
            "file:/sample.json",
            "David|admin",
            List.of(new JsonSearchMatch(9, 14, "David"), new JsonSearchMatch(26, 31, "admin")),
            0);

    String asciiContent =
        """
        root
        ├─ name: "David"
        └─ role: "admin"
        """;

    List<SearchHighlightRange> ranges = projector.asciiRanges(asciiContent, session);

    assertEquals(2, ranges.size());
    assertEquals(asciiContent.indexOf("David"), ranges.get(0).startIndex());
    assertEquals(asciiContent.indexOf("admin"), ranges.get(1).startIndex());
  }

  @Test
  void projectsRawRangesOntoPrettyPrintedJsonDisplay() {
    String rawJson = "{\"name\":\"David\",\"id\":42}";
    RawJsonPresentation presentation = rawJsonPresentationService.present(rawJson);
    int matchStart = rawJson.indexOf("\"name\":\"David\"");
    int matchEnd = matchStart + "\"name\":\"David\"".length();
    JsonSearchSession session =
        new JsonSearchSession(
            "file:/sample.json",
            "\"name\":\"David\"",
            List.of(new JsonSearchMatch(matchStart, matchEnd, "\"name\":\"David\"")),
            0);

    List<SearchHighlightRange> ranges =
        projector.rawRanges(session, presentation.sourceToDisplayBoundaries());

    assertEquals(1, ranges.size());
    assertEquals(
        "\"name\" : \"David\"",
        presentation.content().substring(ranges.get(0).startIndex(), ranges.get(0).endIndex()));
  }
}
