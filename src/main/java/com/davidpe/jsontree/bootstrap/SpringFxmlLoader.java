package com.davidpe.jsontree.bootstrap;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ApplicationContext;

public class SpringFxmlLoader {

    private final ApplicationContext applicationContext;

    public SpringFxmlLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Parent load(String location) {
        URL resource = SpringFxmlLoader.class.getResource(location);
        if (resource == null) {
            throw new IllegalArgumentException("FXML resource not found: " + location);
        }

        FXMLLoader fxmlLoader = new FXMLLoader(resource);
        fxmlLoader.setControllerFactory(applicationContext::getBean);

        try {
            return fxmlLoader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load FXML resource: " + location, exception);
        }
    }
}
