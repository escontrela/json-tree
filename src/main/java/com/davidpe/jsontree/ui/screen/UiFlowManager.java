package com.davidpe.jsontree.ui.screen;

public class UiFlowManager {

    private final UiScreenFactory uiScreenFactory;

    public UiFlowManager(UiScreenFactory uiScreenFactory) {
        this.uiScreenFactory = uiScreenFactory;
    }

    public void show(UiScreenId uiScreenId) {
        uiScreenFactory.create(uiScreenId).show();
    }
}
