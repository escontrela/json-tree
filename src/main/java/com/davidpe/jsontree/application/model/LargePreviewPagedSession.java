package com.davidpe.jsontree.application.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public record LargePreviewPagedSession(
    String sessionId,
    LargePreviewSessionSource source,
    int currentPageIndex,
    Integer totalPages,
    Long totalLogicalLines,
    List<LargePreviewPageRange> pageRanges,
    int residentPageRadius,
    List<LargePreviewPageState> pageStates,
    boolean outlineDigestReady,
    boolean prettyOnLargePreviewEnabled,
    boolean closed) {

  public LargePreviewPagedSession(
      String sessionId,
      LargePreviewSessionSource source,
      int currentPageIndex,
      Integer totalPages,
      Long totalLogicalLines,
      List<LargePreviewPageRange> pageRanges,
      int residentPageRadius,
      List<LargePreviewPageState> pageStates,
      boolean outlineDigestReady,
      boolean closed) {
    this(
        sessionId,
        source,
        currentPageIndex,
        totalPages,
        totalLogicalLines,
        pageRanges,
        residentPageRadius,
        pageStates,
        outlineDigestReady,
        false,
        closed);
  }

  public LargePreviewPagedSession {
    if (sessionId == null || sessionId.isBlank()) {
      throw new IllegalArgumentException("Large-preview session id is required.");
    }
    if (source == null) {
      throw new IllegalArgumentException("Large-preview session source is required.");
    }
    if (currentPageIndex < 0) {
      throw new IllegalArgumentException("Large-preview current page index must be zero or greater.");
    }
    if (totalPages != null && totalPages <= 0) {
      throw new IllegalArgumentException("Large-preview total pages must be positive when known.");
    }
    if (totalPages != null && currentPageIndex >= totalPages) {
      throw new IllegalArgumentException(
          "Large-preview current page index must stay within the known total page count.");
    }
    if (totalLogicalLines != null && totalLogicalLines < 0L) {
      throw new IllegalArgumentException(
          "Large-preview logical line total must be zero or greater when known.");
    }
    if (residentPageRadius < 0) {
      throw new IllegalArgumentException(
          "Large-preview resident page radius must be zero or greater.");
    }

    Map<Integer, LargePreviewPageRange> uniqueRanges = new LinkedHashMap<>();
    for (LargePreviewPageRange pageRange :
        Objects.requireNonNullElse(pageRanges, List.<LargePreviewPageRange>of())) {
      LargePreviewPageRange previous = uniqueRanges.putIfAbsent(pageRange.pageIndex(), pageRange);
      if (previous != null) {
        throw new IllegalArgumentException(
            "Large-preview page ranges must not contain duplicate indexes: "
                + pageRange.pageIndex());
      }
    }
    pageRanges =
        uniqueRanges.values().stream()
            .sorted(Comparator.comparingInt(LargePreviewPageRange::pageIndex))
            .toList();
    if (totalPages != null && !pageRanges.isEmpty() && pageRanges.size() != totalPages) {
      throw new IllegalArgumentException(
          "Large-preview page range count must match known total pages.");
    }
    if (totalLogicalLines != null
        && !pageRanges.isEmpty()
        && pageRanges.getLast().endingLogicalLineExclusive() > totalLogicalLines) {
      throw new IllegalArgumentException(
          "Large-preview page ranges cannot extend beyond the known logical line total.");
    }

    Map<Integer, LargePreviewPageState> uniqueStates = new LinkedHashMap<>();
    for (LargePreviewPageState pageState : Objects.requireNonNullElse(pageStates, List.<LargePreviewPageState>of())) {
      LargePreviewPageState previous = uniqueStates.putIfAbsent(pageState.pageIndex(), pageState);
      if (previous != null) {
        throw new IllegalArgumentException(
            "Large-preview page states must not contain duplicate indexes: " + pageState.pageIndex());
      }
    }
    pageStates =
        uniqueStates.values().stream()
            .sorted(Comparator.comparingInt(LargePreviewPageState::pageIndex))
            .toList();
  }

  public static LargePreviewPagedSession initializing(
      String sessionId, LargePreviewSessionSource source) {
    return initializing(sessionId, source, 0);
  }

  public static LargePreviewPagedSession initializing(
      String sessionId, LargePreviewSessionSource source, int residentPageRadius) {
    return initializing(sessionId, source, residentPageRadius, false);
  }

  public static LargePreviewPagedSession initializing(
      String sessionId,
      LargePreviewSessionSource source,
      int residentPageRadius,
      boolean prettyOnLargePreviewEnabled) {
    return new LargePreviewPagedSession(
        sessionId,
        source,
        0,
        null,
        null,
        List.of(),
        residentPageRadius,
        List.of(LargePreviewPageState.building(0)),
        false,
        prettyOnLargePreviewEnabled,
        false);
  }

  public Optional<LargePreviewPageState> pageState(int pageIndex) {
    return pageStates.stream().filter(state -> state.pageIndex() == pageIndex).findFirst();
  }

  public Optional<LargePreviewPageState> currentPageState() {
    return pageState(currentPageIndex);
  }

  public Optional<LargePreviewPageRange> pageRange(int pageIndex) {
    return pageRanges.stream().filter(range -> range.pageIndex() == pageIndex).findFirst();
  }

  public Optional<LargePreviewPageRange> currentPageRange() {
    return pageRange(currentPageIndex);
  }

  public boolean totalPagesKnown() {
    return totalPages != null;
  }

  public boolean totalLogicalLinesKnown() {
    return totalLogicalLines != null;
  }

  public boolean hasDocumentRanges() {
    return !pageRanges.isEmpty();
  }

  public OptionalInt resolvePageIndexForLogicalLine(long logicalLine) {
    if (logicalLine < 0L || pageRanges.isEmpty()) {
      return OptionalInt.empty();
    }
    return pageRanges.stream()
        .filter(range -> range.containsLogicalLine(logicalLine))
        .mapToInt(LargePreviewPageRange::pageIndex)
        .findFirst();
  }

  public OptionalLong logicalLineForScrollValue(double scrollValue) {
    if (!totalLogicalLinesKnown()) {
      return OptionalLong.empty();
    }
    if (totalLogicalLines == 0L) {
      return OptionalLong.of(0L);
    }
    long maxLogicalLine = Math.max(0L, totalLogicalLines - 1L);
    return OptionalLong.of(Math.round(clampScrollValue(scrollValue) * maxLogicalLine));
  }

  public OptionalInt resolvePageIndexForScrollValue(double scrollValue) {
    OptionalLong logicalLine = logicalLineForScrollValue(scrollValue);
    return logicalLine.isPresent()
        ? resolvePageIndexForLogicalLine(logicalLine.orElseThrow())
        : OptionalInt.empty();
  }

  public double scrollValueForPageStart(int pageIndex) {
    return pageRange(pageIndex)
        .map(pageRange -> scrollValueForLogicalLine(pageRange.startingLogicalLine()))
        .orElse(0.0);
  }

  public double scrollValueForLogicalLine(long logicalLine) {
    if (!totalLogicalLinesKnown() || totalLogicalLines <= 1L) {
      return 0.0;
    }
    long clampedLogicalLine = Math.max(0L, Math.min(logicalLine, totalLogicalLines - 1L));
    return clampedLogicalLine / (double) (totalLogicalLines - 1L);
  }

  public LargePreviewPagedSession withCurrentPageIndex(int nextCurrentPageIndex) {
    assertOpen();
    return new LargePreviewPagedSession(
        sessionId,
        source,
        nextCurrentPageIndex,
        totalPages,
        totalLogicalLines,
        pageRanges,
        residentPageRadius,
        pageStates,
        outlineDigestReady,
        prettyOnLargePreviewEnabled,
        false);
  }

  public LargePreviewPagedSession withPageState(LargePreviewPageState nextPageState) {
    assertOpen();
    Map<Integer, LargePreviewPageState> replacedStates = new LinkedHashMap<>();
    for (LargePreviewPageState pageState : pageStates) {
      replacedStates.put(pageState.pageIndex(), pageState);
    }
    replacedStates.put(nextPageState.pageIndex(), nextPageState);
    return new LargePreviewPagedSession(
        sessionId,
        source,
        currentPageIndex,
        totalPages,
        totalLogicalLines,
        pageRanges,
        residentPageRadius,
        replacedStates.values().stream().toList(),
        outlineDigestReady,
        prettyOnLargePreviewEnabled,
        false);
  }

  public LargePreviewPagedSession withPageStates(Collection<LargePreviewPageState> nextPageStates) {
    LargePreviewPagedSession nextSession = this;
    for (LargePreviewPageState pageState : nextPageStates) {
      nextSession = nextSession.withPageState(pageState);
    }
    return nextSession;
  }

  public LargePreviewPagedSession withKnownTotals(int nextTotalPages, long nextTotalLogicalLines) {
    return withKnownTotals(nextTotalPages, nextTotalLogicalLines, pageRanges);
  }

  public LargePreviewPagedSession withKnownTotals(
      int nextTotalPages, long nextTotalLogicalLines, List<LargePreviewPageRange> nextPageRanges) {
    assertOpen();
    return new LargePreviewPagedSession(
        sessionId,
        source,
        currentPageIndex,
        nextTotalPages,
        nextTotalLogicalLines,
        nextPageRanges,
        residentPageRadius,
        pageStates,
        outlineDigestReady,
        prettyOnLargePreviewEnabled,
        false);
  }

  public LargePreviewPagedSession withPageRanges(List<LargePreviewPageRange> nextPageRanges) {
    assertOpen();
    return new LargePreviewPagedSession(
        sessionId,
        source,
        currentPageIndex,
        totalPages,
        totalLogicalLines,
        nextPageRanges,
        residentPageRadius,
        pageStates,
        outlineDigestReady,
        prettyOnLargePreviewEnabled,
        false);
  }

  public LargePreviewPagedSession withResidentPageRadius(int nextResidentPageRadius) {
    assertOpen();
    return new LargePreviewPagedSession(
        sessionId,
        source,
        currentPageIndex,
        totalPages,
        totalLogicalLines,
        pageRanges,
        nextResidentPageRadius,
        pageStates,
        outlineDigestReady,
        prettyOnLargePreviewEnabled,
        false);
  }

  public LargePreviewPagedSession withOutlineDigestReady(boolean nextOutlineDigestReady) {
    assertOpen();
    return new LargePreviewPagedSession(
        sessionId,
        source,
        currentPageIndex,
        totalPages,
        totalLogicalLines,
        pageRanges,
        residentPageRadius,
        pageStates,
        nextOutlineDigestReady,
        prettyOnLargePreviewEnabled,
        false);
  }

  public LargePreviewPagedSession close() {
    return new LargePreviewPagedSession(
        sessionId,
        source,
        currentPageIndex,
        totalPages,
        totalLogicalLines,
        pageRanges,
        residentPageRadius,
        pageStates,
        outlineDigestReady,
        prettyOnLargePreviewEnabled,
        true);
  }

  private void assertOpen() {
    if (closed) {
      throw new IllegalStateException("Closed large-preview sessions cannot transition.");
    }
  }

  private double clampScrollValue(double scrollValue) {
    if (Double.isNaN(scrollValue) || Double.isInfinite(scrollValue)) {
      return 0.0;
    }
    return Math.max(0.0, Math.min(1.0, scrollValue));
  }
}
