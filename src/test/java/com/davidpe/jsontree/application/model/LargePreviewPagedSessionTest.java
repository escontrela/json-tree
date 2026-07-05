package com.davidpe.jsontree.application.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class LargePreviewPagedSessionTest {

  @Test
  void initializesWithFirstPageBuildingState() {
    LargePreviewPagedSession session =
        LargePreviewPagedSession.initializing(
            "session-1", LargePreviewSessionSource.local(Path.of("/tmp/large.json")));

    assertEquals("session-1", session.sessionId());
    assertEquals(0, session.currentPageIndex());
    assertFalse(session.totalPagesKnown());
    assertFalse(session.totalLogicalLinesKnown());
    assertEquals(0, session.residentPageRadius());
    assertFalse(session.hasDocumentRanges());
    assertEquals(LargePreviewPageStatus.BUILDING, session.currentPageState().orElseThrow().status());
    assertFalse(session.outlineDigestReady());
    assertFalse(session.closed());
  }

  @Test
  void supportsBasicStateTransitions() {
    LargePreviewPagedSession session =
        LargePreviewPagedSession.initializing(
                "session-1", LargePreviewSessionSource.local(Path.of("/tmp/large.json")))
            .withPageState(LargePreviewPageState.available(0, true, true, 240))
            .withPageState(LargePreviewPageState.requested(1))
            .withKnownTotals(
                12,
                2_440L,
                List.of(
                    new LargePreviewPageRange(0, 0L, 240),
                    new LargePreviewPageRange(1, 240L, 240),
                    new LargePreviewPageRange(2, 480L, 240),
                    new LargePreviewPageRange(3, 720L, 240),
                    new LargePreviewPageRange(4, 960L, 240),
                    new LargePreviewPageRange(5, 1_200L, 240),
                    new LargePreviewPageRange(6, 1_440L, 240),
                    new LargePreviewPageRange(7, 1_680L, 240),
                    new LargePreviewPageRange(8, 1_920L, 240),
                    new LargePreviewPageRange(9, 2_160L, 80),
                    new LargePreviewPageRange(10, 2_240L, 100),
                    new LargePreviewPageRange(11, 2_340L, 100)))
            .withCurrentPageIndex(1)
            .withResidentPageRadius(5)
            .withOutlineDigestReady(true);

    assertEquals(12, session.totalPages());
    assertEquals(2_440L, session.totalLogicalLines());
    assertEquals(1, session.currentPageIndex());
    assertTrue(session.hasDocumentRanges());
    assertEquals(5, session.residentPageRadius());
    assertEquals(OptionalInt.of(1), session.resolvePageIndexForLogicalLine(241L));
    assertEquals(240L, session.currentPageRange().orElseThrow().startingLogicalLine());
    assertEquals(LargePreviewPageStatus.REQUESTED, session.currentPageState().orElseThrow().status());
    assertTrue(session.outlineDigestReady());
    assertEquals(List.of(0, 1), session.pageStates().stream().map(LargePreviewPageState::pageIndex).toList());
  }

  @Test
  void rejectsDuplicatePageIndexesInSessionState() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LargePreviewPagedSession(
                "session-1",
                LargePreviewSessionSource.local(Path.of("/tmp/large.json")),
                0,
                null,
                null,
                List.of(),
                0,
                List.of(
                    LargePreviewPageState.building(0),
                    LargePreviewPageState.available(0, true, true, 120)),
                false,
                false));
  }

  @Test
  void rejectsInconsistentKnownTotalsAndPageRanges() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LargePreviewPagedSession(
                "session-1",
                LargePreviewSessionSource.local(Path.of("/tmp/large.json")),
                0,
                2,
                200L,
                List.of(new LargePreviewPageRange(0, 0L, 120)),
                2,
                List.of(LargePreviewPageState.available(0, true, true, 120)),
                false,
                false));
  }

  @Test
  void rejectsClosedSessionTransitions() {
    LargePreviewPagedSession session =
        LargePreviewPagedSession.initializing(
                "session-1", LargePreviewSessionSource.local(Path.of("/tmp/large.json")))
            .close();

    assertThrows(
        IllegalStateException.class,
        () -> session.withPageState(LargePreviewPageState.available(0, true, true, 120)));
    assertThrows(IllegalStateException.class, () -> session.withCurrentPageIndex(1));
    assertThrows(IllegalStateException.class, () -> session.withKnownTotals(2, 240));
  }

  @Test
  void requiresStoredSnapshotIdentityForHistorySources() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LargePreviewSessionSource(
                Path.of("/tmp/history.json"),
                com.davidpe.jsontree.domain.model.JsonDocumentSourceKind.HISTORY,
                " "));
  }
}
