package com.davidpe.jsontree.ui.support;

import java.io.File;
import java.nio.file.Files;
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
                .filter(this::isSupportedImportFile)
                .findFirst();
    }

    private boolean isSupportedImportFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".json")
                || fileName.endsWith(".md")
                || Files.isRegularFile(path);
    }
}
