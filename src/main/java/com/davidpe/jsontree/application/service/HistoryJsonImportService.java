package com.davidpe.jsontree.application.service;

import com.davidpe.jsontree.application.model.HistoryJsonImportResult;
import com.davidpe.jsontree.application.model.HistoryJsonImportStatus;
import com.davidpe.jsontree.application.port.in.ImportHistoryJsonUseCase;
import com.davidpe.jsontree.application.port.out.JsonFileChooserPort;
import com.davidpe.jsontree.domain.model.JsonImportResult;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class HistoryJsonImportService implements ImportHistoryJsonUseCase {

  private final JsonFileChooserPort jsonFileChooserPort;
  private final JsonViewerWorkflowService workflowService;

  public HistoryJsonImportService(
      JsonFileChooserPort jsonFileChooserPort,
      JsonViewerWorkflowService workflowService) {
    this.jsonFileChooserPort = jsonFileChooserPort;
    this.workflowService = workflowService;
  }

  @Override
  public HistoryJsonImportResult importFromDisk() {
    Optional<Path> selectedPath;
    try {
      selectedPath = jsonFileChooserPort.chooseJsonFile();
    } catch (RuntimeException exception) {
      return HistoryJsonImportResult.failure(
          HistoryJsonImportStatus.UNREADABLE_FILE,
          "Unable to inspect local JSON files right now.");
    }

    if (selectedPath.isEmpty()) {
      return HistoryJsonImportResult.cancelled();
    }

    JsonImportResult importResult = workflowService.importFile(selectedPath.get());
    if (!importResult.available()) {
      return HistoryJsonImportResult.failure(
          HistoryJsonImportStatus.UNREADABLE_FILE,
          "Selected JSON file is not available.");
    }

    return workflowService.importIntoHistory(importResult);
  }
}
