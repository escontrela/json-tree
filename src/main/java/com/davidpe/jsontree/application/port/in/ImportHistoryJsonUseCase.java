package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.application.model.HistoryJsonImportResult;

public interface ImportHistoryJsonUseCase {

  HistoryJsonImportResult importFromDisk();
}
