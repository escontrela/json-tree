package com.davidpe.jsontree.application.port.out;

import com.davidpe.jsontree.domain.model.JsonValidationResult;
import java.nio.file.Path;

public interface JsonValidationPort {

    JsonValidationResult validate(Path jsonFilePath);
}
