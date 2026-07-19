package com.davidpe.jsontree.ui.controller;

import com.davidpe.jsontree.application.model.JsonSearchExecutionResult;
import com.davidpe.jsontree.application.model.JsonSearchSession;
import com.davidpe.jsontree.application.service.RegexTextSearchService;
import com.davidpe.jsontree.ui.model.BreadcrumbViewerMode;
import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import com.davidpe.jsontree.ui.model.ZoomViewerSnapshot;
import com.davidpe.jsontree.ui.service.ZoomViewerStateBridge;
import com.davidpe.jsontree.ui.support.JsonBreadcrumbViewportResolver;
import com.davidpe.jsontree.ui.support.RichTextViewerFactory;
import com.davidpe.jsontree.ui.support.RichTextViewerSurface;
import com.davidpe.jsontree.ui.support.SearchHighlightRange;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlan;
import com.davidpe.jsontree.ui.support.ViewerTextRenderPlanSearchOverlay;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Controller for the dedicated secondary zoom window shell.
 */
@Component
public class ZoomWindowController {

  private final RichTextViewerFactory richTextViewerFactory;
  private final ZoomViewerStateBridge zoomViewerStateBridge;
  private final RegexTextSearchService regexTextSearchService;
  private final ViewerTextRenderPlanSearchOverlay renderPlanSearchOverlay;
  private final JsonBreadcrumbViewportResolver breadcrumbViewportResolver;

  private RichTextViewerSurface richTextViewerSurface;
  private Runnable zoomSubscriptionRelease;
  private ZoomViewerSnapshot currentSnapshot;
  private JsonSearchSession searchSession;
  private boolean breadcrumbRefreshPending;

  @FXML private BorderPane rootPane;

  @FXML private Label zoomModeLabel;

  @FXML private Label zoomTitleLabel;

  @FXML private Label zoomMetaLabel;

  @FXML private TextField zoomSearchField;

  @FXML private Button zoomSearchPreviousButton;

  @FXML private Button zoomSearchNextButton;

  @FXML private Label zoomSearchOccurrenceLabel;

  @FXML private Label zoomSearchErrorLabel;

  @FXML private Label zoomBreadcrumbLabel;

  @FXML private StackPane zoomViewerHost;

  @FXML private Label zoomStateLabel;

  public ZoomWindowController(
      RichTextViewerFactory richTextViewerFactory,
      ZoomViewerStateBridge zoomViewerStateBridge,
      RegexTextSearchService regexTextSearchService,
      ViewerTextRenderPlanSearchOverlay renderPlanSearchOverlay,
      JsonBreadcrumbViewportResolver breadcrumbViewportResolver) {
    this.richTextViewerFactory = richTextViewerFactory;
    this.zoomViewerStateBridge = zoomViewerStateBridge;
    this.regexTextSearchService = regexTextSearchService;
    this.renderPlanSearchOverlay = renderPlanSearchOverlay;
    this.breadcrumbViewportResolver = breadcrumbViewportResolver;
  }

  @FXML
  public void initialize() {
    rootPane.getProperties().put("controller", this);
    richTextViewerSurface = richTextViewerFactory.create();
    zoomViewerHost.getChildren().setAll(richTextViewerSurface.view());
    richTextViewerSurface.addViewportChangeListener(this::scheduleBreadcrumbRefresh);
    syncSearchControls();
    showAwaitingDocument();
  }

  @FXML
  void closeWindow() {
    currentWindow().ifPresent(Window::hide);
  }

  @FXML
  void executeSearch() {
    String query = zoomSearchField.getText();
    if (query == null || query.trim().isEmpty()) {
      clearSearchState();
      return;
    }
    if (currentSnapshot == null || !currentSnapshot.renderable() || currentSnapshot.renderPlan() == null) {
      showSearchError("No text is available for search.");
      return;
    }

    JsonSearchExecutionResult result =
        regexTextSearchService.search(searchSourceIdentity(), query, currentRenderedText());
    if (!result.successful()) {
      showSearchError(result.errorMessage());
      return;
    }

    hideSearchError();
    searchSession = result.session();
    syncSearchControls();
    renderSearchAwareSnapshot();
  }

  @FXML
  void showPreviousSearchResult() {
    moveSearchSelection(-1);
  }

  @FXML
  void showNextSearchResult() {
    moveSearchSelection(1);
  }

  public void showAwaitingDocument() {
    presentSnapshot(
        ZoomViewerSnapshot.empty(
            "JSON -> TREE • Zoom",
            "Zoom viewer",
            "Expanded reading surface",
            "Open a JSON in the main workspace to populate this reading surface."));
  }

  public void activate() {
    if (zoomSubscriptionRelease != null) {
      return;
    }
    zoomSubscriptionRelease = zoomViewerStateBridge.subscribe(this::presentSnapshot);
  }

  public void deactivate() {
    if (zoomSubscriptionRelease == null) {
      return;
    }
    zoomSubscriptionRelease.run();
    zoomSubscriptionRelease = null;
  }

  String viewerText() {
    return richTextViewerSurface.text();
  }

  String currentModeLabel() {
    return zoomModeLabel.getText();
  }

  boolean showingDocument() {
    return zoomViewerHost.isVisible();
  }

  boolean viewerEditable() {
    return richTextViewerSurface.editable();
  }

  private void presentSnapshot(ZoomViewerSnapshot snapshot) {
    if (snapshot == null) {
      showAwaitingDocument();
      return;
    }

    currentSnapshot = snapshot;
    zoomModeLabel.setText(snapshot.modeLabel());
    zoomTitleLabel.setText(snapshot.documentTitle());
    applyMeta(snapshot.documentMeta());
    updateWindowTitle(snapshot.windowTitle());

    if (snapshot.renderable() && snapshot.renderPlan() != null) {
      zoomViewerHost.setManaged(true);
      zoomViewerHost.setVisible(true);
      refreshSearchAgainstCurrentSnapshot();
      zoomStateLabel.setManaged(false);
      zoomStateLabel.setVisible(false);
      scheduleBreadcrumbRefresh();
      return;
    }

    searchSession = null;
    syncSearchControls();
    hideSearchError();
    richTextViewerSurface.clear();
    richTextViewerSurface.hide();
    zoomViewerHost.setManaged(false);
    zoomViewerHost.setVisible(false);
    zoomStateLabel.setText(snapshot.emptyStateMessage());
    zoomStateLabel.setManaged(true);
    zoomStateLabel.setVisible(true);
    hideZoomBreadcrumb();
  }

  private void refreshSearchAgainstCurrentSnapshot() {
    String query = zoomSearchField.getText() == null ? "" : zoomSearchField.getText().trim();
    if (query.isEmpty()) {
      searchSession = null;
      syncSearchControls();
      renderBaseSnapshot();
      return;
    }

    JsonSearchExecutionResult result =
        regexTextSearchService.search(searchSourceIdentity(), query, currentRenderedText());
    if (!result.successful()) {
      searchSession = null;
      syncSearchControls();
      showSearchError(result.errorMessage());
      renderBaseSnapshot();
      return;
    }

    hideSearchError();
    searchSession = result.session();
    syncSearchControls();
    renderSearchAwareSnapshot();
  }

  private void renderBaseSnapshot() {
    if (currentSnapshot == null || currentSnapshot.renderPlan() == null) {
      return;
    }
    richTextViewerSurface.showStyledText(
        currentSnapshot.renderPlan().fragments(), currentSnapshot.contentStyleClass());
    richTextViewerSurface.scrollToTop();
  }

  private void renderSearchAwareSnapshot() {
    if (currentSnapshot == null || currentSnapshot.renderPlan() == null) {
      return;
    }

    ViewerTextRenderPlan renderPlan =
        searchSession == null || !searchSession.hasMatches()
            ? currentSnapshot.renderPlan()
            : renderPlanSearchOverlay.apply(currentSnapshot.renderPlan(), currentHighlightRanges());
    richTextViewerSurface.showStyledText(renderPlan.fragments(), currentSnapshot.contentStyleClass());
    if (searchSession != null) {
      searchSession
          .activeMatch()
          .ifPresentOrElse(
              match -> richTextViewerSurface.scrollToOffset(match.startIndex()),
              richTextViewerSurface::scrollToTop);
    } else {
      richTextViewerSurface.scrollToTop();
    }
    scheduleBreadcrumbRefresh();
  }

  private String currentRenderedText() {
    return currentSnapshot == null || currentSnapshot.renderPlan() == null
        ? ""
        : renderPlanSearchOverlay.flatten(currentSnapshot.renderPlan());
  }

  private void moveSearchSelection(int delta) {
    if (searchSession == null || !searchSession.hasMatches()) {
      return;
    }
    if (searchSession.totalMatches() > 1) {
      searchSession =
          searchSession.withActiveMatchIndex(
              Math.floorMod(searchSession.activeMatchIndex() + delta, searchSession.totalMatches()));
    }
    syncSearchControls();
    renderSearchAwareSnapshot();
  }

  private List<SearchHighlightRange> currentHighlightRanges() {
    if (searchSession == null || !searchSession.hasMatches()) {
      return List.of();
    }

    Optional<com.davidpe.jsontree.application.model.JsonSearchMatch> activeMatch =
        searchSession.activeMatch();
    return searchSession.matches().stream()
        .map(
            match ->
                new SearchHighlightRange(
                    match.startIndex(),
                    match.endIndex(),
                    activeMatch.map(match::equals).orElse(false)))
        .toList();
  }

  private void clearSearchState() {
    searchSession = null;
    hideSearchError();
    syncSearchControls();
    renderBaseSnapshot();
  }

  private void syncSearchControls() {
    if (searchSession == null) {
      zoomSearchOccurrenceLabel.setText("Ready");
      zoomSearchPreviousButton.setDisable(true);
      zoomSearchNextButton.setDisable(true);
      return;
    }
    zoomSearchOccurrenceLabel.setText(
        searchSession.hasMatches()
            ? (searchSession.activeMatchIndex() + 1) + " of " + searchSession.totalMatches()
            : "0 matches");
    boolean navigationEnabled = searchSession.totalMatches() > 1;
    zoomSearchPreviousButton.setDisable(!navigationEnabled);
    zoomSearchNextButton.setDisable(!navigationEnabled);
  }

  private void showSearchError(String errorMessage) {
    zoomSearchErrorLabel.setText(errorMessage == null ? "Invalid regular expression." : errorMessage);
    zoomSearchErrorLabel.setManaged(true);
    zoomSearchErrorLabel.setVisible(true);
  }

  private void hideSearchError() {
    zoomSearchErrorLabel.setText("");
    zoomSearchErrorLabel.setManaged(false);
    zoomSearchErrorLabel.setVisible(false);
  }

  private String searchSourceIdentity() {
    if (currentSnapshot == null) {
      return "zoom";
    }
    return currentSnapshot.windowTitle() + "::" + currentSnapshot.modeLabel();
  }

  private void scheduleBreadcrumbRefresh() {
    if (breadcrumbRefreshPending) {
      return;
    }
    breadcrumbRefreshPending = true;
    Platform.runLater(
        () -> {
          breadcrumbRefreshPending = false;
          refreshBreadcrumb();
        });
  }

  private void refreshBreadcrumb() {
    if (currentSnapshot == null
        || !currentSnapshot.renderable()
        || currentSnapshot.largePreview()
        || currentSnapshot.breadcrumbModel() == null
        || !currentSnapshot.breadcrumbModel().available()) {
      hideZoomBreadcrumb();
      return;
    }

    breadcrumbViewportResolver
        .resolve(
            currentSnapshot.breadcrumbModel(),
            breadcrumbViewerMode(currentSnapshot.presentationMode()),
            richTextViewerSurface.firstVisibleParagraphIndex())
        .map(com.davidpe.jsontree.application.model.JsonBreadcrumbPath::displayLabel)
        .filter(text -> !text.isBlank())
        .ifPresentOrElse(this::showZoomBreadcrumb, this::hideZoomBreadcrumb);
  }

  private BreadcrumbViewerMode breadcrumbViewerMode(ViewerPresentationMode presentationMode) {
    if (presentationMode == ViewerPresentationMode.RAW_JSON) {
      return BreadcrumbViewerMode.RAW_JSON;
    }
    if (presentationMode == ViewerPresentationMode.STRUCTURE) {
      return BreadcrumbViewerMode.STRUCTURE;
    }
    return BreadcrumbViewerMode.ASCII_TREE;
  }

  private void showZoomBreadcrumb(String breadcrumbText) {
    zoomBreadcrumbLabel.setText(breadcrumbText);
    zoomBreadcrumbLabel.setManaged(true);
    zoomBreadcrumbLabel.setVisible(true);
  }

  private void hideZoomBreadcrumb() {
    zoomBreadcrumbLabel.setText("");
    zoomBreadcrumbLabel.setManaged(false);
    zoomBreadcrumbLabel.setVisible(false);
  }

  private void applyMeta(String documentMeta) {
    boolean visible = documentMeta != null && !documentMeta.isBlank();
    zoomMetaLabel.setText(visible ? documentMeta : "");
    zoomMetaLabel.setManaged(visible);
    zoomMetaLabel.setVisible(visible);
  }

  private void updateWindowTitle(String windowTitle) {
    currentWindow()
        .filter(Stage.class::isInstance)
        .map(Stage.class::cast)
        .ifPresent(stage -> stage.setTitle(windowTitle));
  }

  private Optional<Window> currentWindow() {
    if (rootPane.getScene() == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(rootPane.getScene().getWindow());
  }
}
