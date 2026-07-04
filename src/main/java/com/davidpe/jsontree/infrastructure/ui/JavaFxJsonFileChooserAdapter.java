package com.davidpe.jsontree.infrastructure.ui;

import com.davidpe.jsontree.application.port.out.JsonFileChooserPort;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class JavaFxJsonFileChooserAdapter implements JsonFileChooserPort {

  private final ObjectProvider<Stage> stageProvider;

  public JavaFxJsonFileChooserAdapter(ObjectProvider<Stage> stageProvider) {
    this.stageProvider = stageProvider;
  }

  @Override
  public Optional<Path> chooseJsonFile() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Import JSON");
    chooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("JSON files", "*.json"),
            new FileChooser.ExtensionFilter("All files", "*.*"));

    File selectedFile = chooser.showOpenDialog(stageProvider.getIfAvailable());
    return Optional.ofNullable(selectedFile).map(File::toPath);
  }
}
