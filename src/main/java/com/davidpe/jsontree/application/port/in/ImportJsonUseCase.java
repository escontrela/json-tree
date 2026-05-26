package com.davidpe.jsontree.application.port.in;

import java.nio.file.Path;

public interface ImportJsonUseCase {

    void importFile(Path jsonFilePath);
}
