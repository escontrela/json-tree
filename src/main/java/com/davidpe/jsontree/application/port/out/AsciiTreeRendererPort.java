package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import java.nio.file.Path;

public interface AsciiTreeRendererPort {

    AsciiTreeDocument render(Path jsonFilePath);
}
