package com.davidpe.jsontree.bootstrap;

import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenFactory;
import javafx.stage.Stage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class ApplicationConfig {

    @Bean
    public String applicationTitle() {
        return "JSON -> TREE";
    }

    @Bean
    public SpringFxmlLoader springFxmlLoader(org.springframework.context.ApplicationContext applicationContext) {
        return new SpringFxmlLoader(applicationContext);
    }

    @Bean
    @Lazy
    public UiScreenFactory uiScreenFactory(Stage primaryStage, SpringFxmlLoader springFxmlLoader) {
        return new UiScreenFactory(primaryStage, springFxmlLoader);
    }

    @Bean
    @Lazy
    public UiFlowManager uiFlowManager(UiScreenFactory uiScreenFactory) {
        return new UiFlowManager(uiScreenFactory);
    }
}
