package com.davidpe.jsontree.ui.support;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DroppedJsonPathResolver {

    public Optional<Path> resolve(List<File> files) {
        if (files == null || files.isEmpty()) {
            return Optional.empty();
        }
        return files.stream()
                .map(File::toPath)
                .filter(this::isSupportedJsonFile)
                .findFirst();
    }

    private boolean isSupportedJsonFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }
}
