package com.davidpe.jsontree.ui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.application.port.in.SearchHistoryJsonUseCase;
import com.davidpe.jsontree.infrastructure.config.LargePreviewProperties;
import com.davidpe.jsontree.application.service.JsonInspectionModeResolver;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenFactory;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import com.davidpe.jsontree.ui.service.CurlEditorModalCoordinator;
import com.davidpe.jsontree.ui.support.HistoryArchiveViewStateResolver;
import com.davidpe.jsontree.ui.support.HistoryCurlEditAvailabilityResolver;
import com.davidpe.jsontree.ui.support.HistoryFavoritePresentationResolver;
import com.davidpe.jsontree.ui.support.HistoryFavoritesViewStateResolver;
import com.davidpe.jsontree.ui.support.LargePreviewIndicatorResolver;
import org.junit.jupiter.api.Test;

class HistoryNavigationControllerTest {

  @Test
  void historyBackActionNavigatesToMainScreen() {
    RecordingUiFlowManager uiFlowManager = new RecordingUiFlowManager();
    HistoryScreenController controller = historyController(unusedSearchUseCase(), uiFlowManager);

    controller.backToMain();

    assertEquals(UiScreenId.MAIN, uiFlowManager.lastShown());
  }

  private HistoryScreenController historyController(
      SearchHistoryJsonUseCase searchUseCase, UiFlowManager uiFlowManager) {
    return new HistoryScreenController(
        null,
        null,
        searchUseCase,
        null,
        new HistoryFavoritePresentationResolver(),
        new HistoryArchiveViewStateResolver(new HistoryFavoritesViewStateResolver()),
        new HistoryCurlEditAvailabilityResolver(),
        new LargePreviewIndicatorResolver(new JsonInspectionModeResolver(new LargePreviewProperties())),
        new NoOpCurlEditorModalCoordinator(),
        uiFlowManager);
  }

  private SearchHistoryJsonUseCase unusedSearchUseCase() {
    return (query, includeNonFavorites) -> com.davidpe.jsontree.application.model.HistoryJsonSearchResult.blocked(java.util.List.of());
  }

  private static final class RecordingUiFlowManager extends UiFlowManager {
    private UiScreenId lastShown;

    private RecordingUiFlowManager() {
      super((UiScreenFactory) null);
    }

    @Override
    public void show(UiScreenId uiScreenId) {
      lastShown = uiScreenId;
    }

    private UiScreenId lastShown() {
      return lastShown;
    }
  }

  private static final class NoOpCurlEditorModalCoordinator
      implements CurlEditorModalCoordinator {
    @Override
    public void openNew(Runnable onSuccess) {}

    @Override
    public void openPrefilled(String curlCommand, Runnable onSuccess) {}
  }
}
