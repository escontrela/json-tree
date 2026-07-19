package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.domain.model.DocumentFormat;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.domain.model.JsonDocumentSourceKind;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenFactory;
import com.davidpe.jsontree.ui.service.CurlEditorModalCoordinator;
import com.davidpe.jsontree.ui.support.HistoryArchiveViewStateResolver;
import com.davidpe.jsontree.ui.support.HistoryFavoritesViewStateResolver;
import com.davidpe.jsontree.ui.support.HistoryCurlEditAvailabilityResolver;
import com.davidpe.jsontree.ui.support.HistoryFavoritePresentationResolver;
import com.davidpe.jsontree.ui.support.LargePreviewIndicatorResolver;
import com.davidpe.jsontree.application.service.JsonInspectionModeResolver;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class HistoryScreenCurlActionsTest {

  @Test
  void opensEmptyModalForNewCurlAction() {
    RecordingCurlEditorModalCoordinator coordinator = new RecordingCurlEditorModalCoordinator();
    HistoryScreenController controller = historyController(coordinator);

    controller.openNewCurl();

    assertEquals("", coordinator.lastPrefilledCommand);
    assertEquals(1, coordinator.openCount);
  }

  @Test
  void opensPrefilledModalOnlyForCurlBackedEntries() {
    RecordingCurlEditorModalCoordinator coordinator = new RecordingCurlEditorModalCoordinator();
    HistoryScreenController controller = historyController(coordinator);

    controller.openEditCurl(curlEntry("curl https://example.com/items"));
    controller.openEditCurl(localEntry());

    assertEquals("curl https://example.com/items", coordinator.lastPrefilledCommand);
    assertEquals(1, coordinator.openCount);
  }

  private HistoryScreenController historyController(
      RecordingCurlEditorModalCoordinator coordinator) {
    return new HistoryScreenController(
        null,
        null,
        null,
        null,
        new HistoryFavoritePresentationResolver(),
        new HistoryArchiveViewStateResolver(new HistoryFavoritesViewStateResolver()),
        new HistoryCurlEditAvailabilityResolver(),
        new LargePreviewIndicatorResolver(new JsonInspectionModeResolver(new LargePreviewProperties())),
        coordinator,
        new UiFlowManager((UiScreenFactory) null));
  }

  private ImportedJsonFile curlEntry(String curlCommand) {
    return new ImportedJsonFile(
        "stored.json",
        "remote.json",
        Instant.parse("2026-07-19T14:00:00Z"),
        128L,
        8,
        true,
        false,
        DocumentFormat.JSON,
        JsonDocumentSourceKind.CURL,
        curlCommand);
  }

  private ImportedJsonFile localEntry() {
    return new ImportedJsonFile(
        "stored.json",
        "local.json",
        Instant.parse("2026-07-19T14:00:00Z"),
        128L,
        8,
        true,
        false,
        DocumentFormat.JSON,
        JsonDocumentSourceKind.LOCAL_FILE,
        null);
  }

  private static final class RecordingCurlEditorModalCoordinator
      implements CurlEditorModalCoordinator {

    private int openCount;
    private String lastPrefilledCommand = "__unset__";

    @Override
    public void openNew(Runnable onSuccess) {
      openCount++;
      lastPrefilledCommand = "";
    }

    @Override
    public void openPrefilled(String curlCommand, Runnable onSuccess) {
      openCount++;
      lastPrefilledCommand = curlCommand;
    }
  }
}
