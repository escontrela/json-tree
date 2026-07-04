package com.davidpe.jsontree.application.port.out;

import java.nio.file.Path;
import java.util.Optional;

public interface JsonFileChooserPort {

  Optional<Path> chooseJsonFile();
}
