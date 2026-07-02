package com.davidpe.jsontree.application.port.in;

import com.davidpe.jsontree.application.model.ClipboardJsonImportResult;

public interface ImportClipboardJsonUseCase {

  ClipboardJsonImportResult importFromClipboard();
}
