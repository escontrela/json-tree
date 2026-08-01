package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.model.JsonInspectionMode;
import com.davidpe.jsontree.application.model.JsonBreadcrumbModel;
import com.davidpe.jsontree.application.model.JsonViewerCapabilities;
import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.application.service.JsonViewerWorkflowService;
import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import com.davidpe.jsontree.domain.model.JsonValidationResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import com.davidpe.jsontree.ui.service.TypewriterLabelRevealService;
import com.davidpe.jsontree.ui.service.ZoomWindowCoordinator;
import com.davidpe.jsontree.ui.service.ZoomViewerStateBridge;
import com.davidpe.jsontree.ui.support.ZoomActionAvailabilityResolver;
import com.davidpe.jsontree.ui.support.ZoomViewerSnapshotFactory;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlan;
import java.util.List;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MainWindowZoomActionTest {

  @Test
  void ignoresZoomOpenWhenCurrentViewIsNotRenderable() {
    RecordingZoomWindowCoordinator coordinator = new RecordingZoomWindowCoordinator();
    MainWindowController controller =
        new MainWindowController(
            null,
            null,
            null,
            new StaticWorkflowService(Optional.empty()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new TypewriterLabelRevealService(),
            null,
            new ZoomActionAvailabilityResolver(),
            coordinator,
            new ZoomViewerStateBridge(),
            new ZoomViewerSnapshotFactory(),
            null);

    controller.openZoomViewer();

    assertEquals(0, coordinator.openCount());
    assertEquals(null, coordinator.lastSnapshot());
  }

  @Test
  void opensZoomWorkflowWhenCurrentViewIsRenderable() {
    RecordingZoomWindowCoordinator coordinator = new RecordingZoomWindowCoordinator();
    MainWindowController controller =
        new MainWindowController(
            null,
            null,
            null,
            new StaticWorkflowService(Optional.of(renderableResult())),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new TypewriterLabelRevealService(),
            null,
            new ZoomActionAvailabilityResolver(),
            coordinator,
            new ZoomViewerStateBridge(),
            new ZoomViewerSnapshotFactory(),
            null);

    controller.openZoomViewer();

    assertEquals(1, coordinator.openCount());
  }

  @Test
  void republishesTheLatestRenderableZoomSnapshotBeforeOpeningZoom() {
    RecordingZoomWindowCoordinator coordinator = new RecordingZoomWindowCoordinator();
    ZoomViewerStateBridge bridge = new ZoomViewerStateBridge();
    MainWindowController controller =
        new MainWindowController(
            null,
            null,
            null,
            new StaticWorkflowService(Optional.of(renderableResult())),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new TypewriterLabelRevealService(),
            null,
            new ZoomActionAvailabilityResolver(),
            coordinator,
            bridge,
            new ZoomViewerSnapshotFactory(),
            null);

    ZoomViewerSnapshot snapshot =
        ZoomViewerSnapshot.renderable(
            false,
            "JSON -> TREE • Zoom • sample.json",
            "ASCII tree",
            "sample.json",
            "1.0 KB • local import",
            "root",
            ViewerTextRenderPlan.normal(List.of()),
            "tree-content",
            ViewerPresentationMode.ASCII_TREE,
            JsonBreadcrumbModel.unavailable(),
            com.davidpe.jsontree.application.model.JsonOutlineModel.empty());
    setField(controller, "currentZoomSnapshot", snapshot);

    controller.openZoomViewer();

    assertEquals(snapshot, bridge.currentSnapshot());
    assertEquals(1, coordinator.openCount());
    assertEquals(snapshot, coordinator.lastSnapshot());
  }

  private JsonViewerLoadResult renderableResult() {
    return new JsonViewerLoadResult(
        new JsonImportResult(
            Path.of("/tmp/sample.json"),
            "sample.json",
            1024L,
            true,
            true,
            true,
            JsonDocumentSourceKind.LOCAL_FILE),
        new JsonValidationResult(JsonValidationStatus.VALID, "Valid JSON", null, null),
        new AsciiTreeDocument("root", "root\n└─ id: 1", 2),
        null,
        JsonInspectionMode.FULL,
        JsonViewerCapabilities.full(),
        null);
  }

  private static final class StaticWorkflowService extends JsonViewerWorkflowService {

    private final Optional<JsonViewerLoadResult> currentView;

    private StaticWorkflowService(Optional<JsonViewerLoadResult> currentView) {
      super(null, null, null, null, null);
      this.currentView = currentView;
    }

    @Override
    public Optional<JsonViewerLoadResult> currentView() {
      return currentView;
    }
  }

  private static final class RecordingZoomWindowCoordinator implements ZoomWindowCoordinator {

    private int openCount;
    private ZoomViewerSnapshot lastSnapshot;

    @Override
    public void openOrFocus(ZoomViewerSnapshot initialSnapshot) {
      openCount++;
      lastSnapshot = initialSnapshot;
    }

    private int openCount() {
      return openCount;
    }

    private ZoomViewerSnapshot lastSnapshot() {
      return lastSnapshot;
    }
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }
}
