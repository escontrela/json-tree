package com.davidpe.jsontree.application.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
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
            .withKnownTotals(12, 2_440L)
            .withCurrentPageIndex(1)
            .withOutlineDigestReady(true);

    assertEquals(12, session.totalPages());
    assertEquals(2_440L, session.totalLogicalLines());
    assertEquals(1, session.currentPageIndex());
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
                List.of(
                    LargePreviewPageState.building(0),
                    LargePreviewPageState.available(0, true, true, 120)),
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
