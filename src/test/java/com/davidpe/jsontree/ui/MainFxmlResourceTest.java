package com.davidpe.jsontree.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MainFxmlResourceTest {

    @Test
    @DisplayName("main FXML resource is present on the classpath")
    void mainFxmlResourceIsPresentOnTheClasspath() {
        assertNotNull(
                MainFxmlResourceTest.class.getResource("/com/davidpe/jsontree/ui/main.fxml")
        );
    }

    @Test
    @DisplayName("history FXML resource is present on the classpath")
    void historyFxmlResourceIsPresentOnTheClasspath() {
        assertNotNull(
                MainFxmlResourceTest.class.getResource("/com/davidpe/jsontree/ui/history.fxml")
        );
    }
}
