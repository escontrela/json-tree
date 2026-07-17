package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.application.model.LargePreviewMaterializationSnapshot;
import com.davidpe.jsontree.application.model.LargePreviewPageContent;
import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import com.davidpe.jsontree.application.model.LargePreviewSessionSource;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public interface LargePreviewSessionStorePort {

  LargePreviewMaterializationSnapshot materialize(
      String sessionId,
      LargePreviewSessionSource source,
      Consumer<LargePreviewPageDescriptor> onPageAvailable);

  Optional<LargePreviewPageContent> readPage(LargePreviewPageDescriptor descriptor);

  void deleteSessionStorage(Path sessionStoragePath);
}
