package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import java.nio.file.Path;

public interface AsciiTreeRendererPort {

    AsciiTreeDocument render(Path jsonFilePath);

    default AsciiTreeDocument renderRawJson(String rawJson) {
        throw new UnsupportedOperationException("In-memory JSON rendering is not implemented.");
    }

    default AsciiTreeDocument renderLargePreview(Path jsonFilePath) {
        return render(jsonFilePath);
    }
}
