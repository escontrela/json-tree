package com.davidpe.jsontree.ui.screen;

public enum UiScreenId {
    MAIN("/com/davidpe/jsontree/ui/main.fxml", 980, 680),
    HISTORY("/com/davidpe/jsontree/ui/history.fxml", 980, 680);

    private final String fxmlPath;
    private final double width;
    private final double height;

    UiScreenId(String fxmlPath, double width, double height) {
        this.fxmlPath = fxmlPath;
        this.width = width;
        this.height = height;
    }

    public String fxmlPath() {
        return fxmlPath;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }
}
