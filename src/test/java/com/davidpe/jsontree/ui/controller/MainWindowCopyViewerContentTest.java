package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.port.out.ClipboardPort;
import com.davidpe.jsontree.ui.service.TypewriterLabelRevealService;
import com.davidpe.jsontree.ui.service.ZoomViewerStateBridge;
import com.davidpe.jsontree.ui.support.JavaFxThreadTestSupport;
import com.davidpe.jsontree.ui.support.RichTextViewerFactory;
import com.davidpe.jsontree.ui.support.RichTextViewerSurface;
import com.davidpe.jsontree.ui.support.ZoomActionAvailabilityResolver;
import com.davidpe.jsontree.ui.support.ZoomViewerSnapshotFactory;
import java.lang.reflect.Field;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Test;

class MainWindowCopyViewerContentTest {

  @Test
  void copiesTheCurrentlyRenderedRichTextViewerContent() {
    JavaFxThreadTestSupport.runOnFxThread(
        () -> {
          RecordingClipboard clipboard = new RecordingClipboard();
          MainWindowController controller = controller(clipboard);
          RichTextViewerSurface surface = new RichTextViewerFactory().create();
          surface.showText("{\"active\":true}", "raw-json-content");

          setField(controller, "copyTreeButton", new Button("Copy tree"));
          ((Button) getField(controller, "copyTreeButton")).setDisable(false);
          setField(controller, "richTextViewerSurface", surface);

          controller.copyTree();

          assertEquals("{\"active\":true}", clipboard.copiedText());
        });
  }

  private MainWindowController controller(ClipboardPort clipboardPort) {
    return new MainWindowController(
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
        clipboardPort,
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
        () -> {},
        new ZoomViewerStateBridge(),
        new ZoomViewerSnapshotFactory(),
        null);
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }

  private Object getField(Object target, String fieldName) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }

  private static final class RecordingClipboard implements ClipboardPort {

    private String copiedText;

    @Override
    public java.util.Optional<String> readText() {
      return java.util.Optional.empty();
    }

    @Override
    public void copy(String text) {
      copiedText = text;
    }

    private String copiedText() {
      return copiedText;
    }
  }
}
