package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.domain.model.JsonImportResult;
import java.nio.file.Path;

public interface ImportJsonUseCase {

    JsonImportResult importFile(Path jsonFilePath);
}
