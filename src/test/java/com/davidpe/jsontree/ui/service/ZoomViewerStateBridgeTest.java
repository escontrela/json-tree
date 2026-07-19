package com.davidpe.jsontree.ui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import com.davidpe.jsontree.ui.support.ViewerTextRenderFragment;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlan;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoomViewerStateBridgeTest {

  @Test
  void replaysTheCurrentSnapshotAndStopsSendingUpdatesAfterUnsubscribe() {
    ZoomViewerStateBridge bridge = new ZoomViewerStateBridge();
    List<String> firstSubscriptionModes = new ArrayList<>();
    Runnable release =
        bridge.subscribe(snapshot -> firstSubscriptionModes.add(snapshot.modeLabel()));

    bridge.publish(renderable("ASCII tree", "sample-a.json"));
    bridge.publish(renderable("Raw JSON", "sample-a.json"));
    bridge.publish(renderable("ASCII tree", "sample-b.json"));
    bridge.publish(
        ZoomViewerSnapshot.empty(
            "JSON -> TREE • Zoom",
            "Zoom viewer",
            "Expanded reading surface",
            "No JSON loaded"));
    release.run();
    bridge.publish(renderable("Raw JSON", "sample-c.json"));

    assertIterableEquals(
        List.of("Zoom viewer", "ASCII tree", "Raw JSON", "ASCII tree", "Zoom viewer"),
        firstSubscriptionModes);
  }

  @Test
  void reusesTheLatestSnapshotWhenTheZoomWindowSubscribesAgain() {
    ZoomViewerStateBridge bridge = new ZoomViewerStateBridge();
    bridge.publish(renderable("ASCII tree", "sample-a.json"));
    Runnable firstRelease = bridge.subscribe(snapshot -> {});
    firstRelease.run();
    bridge.publish(renderable("Raw JSON", "sample-b.json"));

    List<String> replayedModes = new ArrayList<>();
    Runnable secondRelease = bridge.subscribe(snapshot -> replayedModes.add(snapshot.modeLabel()));

    assertEquals(List.of("Raw JSON"), replayedModes);
    secondRelease.run();
  }

  @Test
  void keepsStructureSnapshotsInTheSameSharedBridgeLifecycle() {
    ZoomViewerStateBridge bridge = new ZoomViewerStateBridge();

    bridge.publish(renderable("Structure", "schema-a.json"));

    List<String> replayedModes = new ArrayList<>();
    Runnable release = bridge.subscribe(snapshot -> replayedModes.add(snapshot.modeLabel()));

    assertEquals(List.of("Structure"), replayedModes);
    release.run();
  }

  @Test
  void replaysRenderedAndRawMarkdownSnapshotsWithoutLosingTheModeIdentity() {
    ZoomViewerStateBridge bridge = new ZoomViewerStateBridge();

    bridge.publish(renderable("Markdown", "readme.md"));
    bridge.publish(renderable("Raw Markdown", "readme.md"));

    List<String> replayedModes = new ArrayList<>();
    Runnable release = bridge.subscribe(snapshot -> replayedModes.add(snapshot.modeLabel()));

    assertEquals(List.of("Raw Markdown"), replayedModes);
    release.run();
  }

  private ZoomViewerSnapshot renderable(String modeLabel, String fileName) {
    return ZoomViewerSnapshot.renderable(
        false,
        "JSON -> TREE • Zoom • " + fileName,
        modeLabel,
        fileName,
        "1.0 KB • local import",
        ViewerTextRenderPlan.normal(
            List.of(
                new ViewerTextRenderFragment(
                    "root\n└─ id: 1", "tree-default", "#2d333a", false, false))),
        "tree-content",
        switch (modeLabel) {
          case "Raw JSON" -> ViewerPresentationMode.RAW_JSON;
          case "Raw Markdown" -> ViewerPresentationMode.RAW_MARKDOWN;
          case "Markdown" -> ViewerPresentationMode.MARKDOWN_RENDERED;
          case "Structure" -> ViewerPresentationMode.STRUCTURE;
          default -> ViewerPresentationMode.ASCII_TREE;
        },
        JsonBreadcrumbModel.unavailable());
  }
}
