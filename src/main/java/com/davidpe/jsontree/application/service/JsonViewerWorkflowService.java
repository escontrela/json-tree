package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.port.in.ImportJsonUseCase;
import com.davidpe.jsontree.application.port.in.OpenHistoryUseCase;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class JsonViewerWorkflowService implements ImportJsonUseCase, OpenHistoryUseCase {

    @Override
    public void importFile(Path jsonFilePath) {
        throw new UnsupportedOperationException("Pending implementation.");
    }

    @Override
    public void openHistory() {
        throw new UnsupportedOperationException("Pending implementation.");
    }
}
