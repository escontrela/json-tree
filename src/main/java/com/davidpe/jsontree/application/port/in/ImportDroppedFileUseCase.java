package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.application.model.DroppedFileImportResult;
import java.nio.file.Path;

/**
 * Resolves a dropped file into either a local document import or a curl-backed fetch.
 */
public interface ImportDroppedFileUseCase {

  DroppedFileImportResult importDroppedFile(Path path);
}
