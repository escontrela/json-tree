package com.davidpe.jsontree.ui.service;

import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Shared presentation bridge between the main viewer and the zoom window.
 */
@Component
public class ZoomViewerStateBridge {

  private final ObjectProperty<ZoomViewerSnapshot> currentSnapshot =
      new SimpleObjectProperty<>(
          ZoomViewerSnapshot.empty(
              "JSON -> TREE • Zoom",
              "Zoom viewer",
              "Expanded reading surface",
              "Open a JSON in the main workspace to populate this reading surface."));

  public ZoomViewerSnapshot currentSnapshot() {
    return currentSnapshot.get();
  }

  public void publish(ZoomViewerSnapshot snapshot) {
    currentSnapshot.set(snapshot);
  }

  public void addListener(ChangeListener<ZoomViewerSnapshot> listener) {
    currentSnapshot.addListener(listener);
  }

  public void removeListener(ChangeListener<ZoomViewerSnapshot> listener) {
    currentSnapshot.removeListener(listener);
  }

  public Runnable subscribe(Consumer<ZoomViewerSnapshot> consumer) {
    Objects.requireNonNull(consumer, "consumer must not be null");
    ChangeListener<ZoomViewerSnapshot> listener =
        (unused, oldValue, newValue) -> consumer.accept(newValue);
    currentSnapshot.addListener(listener);
    consumer.accept(currentSnapshot());
    return () -> currentSnapshot.removeListener(listener);
  }
}
