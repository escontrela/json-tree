package com.davidpe.jsontree.bootstrap;

import com.davidpe.jsontree.infrastructure.config.AppDataProperties;
import com.davidpe.jsontree.ui.screen.UiFlowManager;
import com.davidpe.jsontree.ui.screen.UiScreenId;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(
        scanBasePackages = "com.davidpe.jsontree",
        exclude = {
                DataSourceAutoConfiguration.class,
                FlywayAutoConfiguration.class
        }
)
@EnableConfigurationProperties(AppDataProperties.class)
public class JsonTreeApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(JsonTreeApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(String[]::new));
    }

    @Override
    public void start(Stage primaryStage) {
        applicationContext.getBeanFactory().registerSingleton("primaryStage", primaryStage);

        String applicationTitle = applicationContext.getBean("applicationTitle", String.class);
        primaryStage.setTitle(applicationTitle);

        UiFlowManager uiFlowManager = applicationContext.getBean(UiFlowManager.class);
        uiFlowManager.show(UiScreenId.MAIN);
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
