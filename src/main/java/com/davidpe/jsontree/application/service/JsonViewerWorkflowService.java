package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.port.in.ImportJsonUseCase;
import com.davidpe.jsontree.application.port.in.OpenHistoryUseCase;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class JsonViewerWorkflowService implements ImportJsonUseCase, OpenHistoryUseCase {

    @Override
    public JsonImportResult importFile(Path jsonFilePath) {
        Path normalizedPath = jsonFilePath.toAbsolutePath().normalize();
        boolean exists = Files.exists(normalizedPath);
        boolean readable = Files.isReadable(normalizedPath);
        boolean regularFile = Files.isRegularFile(normalizedPath);

        return new JsonImportResult(
                normalizedPath,
                normalizedPath.getFileName().toString(),
                resolveSize(normalizedPath, exists, regularFile),
                exists,
                readable,
                regularFile
        );
    }

    @Override
    public void openHistory() {
        throw new UnsupportedOperationException("Pending implementation.");
    }

    private long resolveSize(Path path, boolean exists, boolean regularFile) {
        if (!exists || !regularFile) {
            return 0L;
        }
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return 0L;
        }
    }
}
