package com.davidpe.jsontree.ui.screen;

public class UiFlowManager {

  private final UiScreenFactory uiScreenFactory;
  private UiScreen currentScreen;

  public UiFlowManager(UiScreenFactory uiScreenFactory) {
    this.uiScreenFactory = uiScreenFactory;
  }

  public void show(UiScreenId uiScreenId) {
    UiScreen nextScreen = uiScreenFactory.create(uiScreenId);
    if (currentScreen != null) {
      currentScreen.controller().onHide();
    }
    nextScreen.show();
    currentScreen = nextScreen;
  }
}
