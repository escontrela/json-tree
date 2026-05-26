package com.davidpe.jsontree.infrastructure.persistence;

import com.davidpe.jsontree.application.port.out.JsonHistoryRepository;
import com.davidpe.jsontree.domain.model.ImportedJsonFile;
import com.davidpe.jsontree.infrastructure.config.AppDataProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FileSystemJsonHistoryRepository implements JsonHistoryRepository {

    private static final TypeReference<List<ImportedJsonFile>> HISTORY_LIST_TYPE = new TypeReference<>() {
    };

    private final AppDataProperties appDataProperties;
    private final ObjectMapper objectMapper;

    public FileSystemJsonHistoryRepository(AppDataProperties appDataProperties, ObjectMapper objectMapper) {
        this.appDataProperties = appDataProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ImportedJsonFile> findAll() {
        if (!Files.exists(metadataPath())) {
            return new ArrayList<>();
        }
        try {
            List<ImportedJsonFile> entries = objectMapper.readValue(metadataPath().toFile(), HISTORY_LIST_TYPE);
            entries.sort(Comparator.comparing(ImportedJsonFile::importedAt));
            return entries;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read JSON history metadata.", exception);
        }
    }

    @Override
    public Optional<ImportedJsonFile> findByStoredName(String storedName) {
        return findAll().stream()
                .filter(entry -> entry.storedName().equals(storedName))
                .findFirst();
    }

    @Override
    public Optional<String> readStoredJson(String storedName) {
        Path snapshotPath = historyDirectory().resolve(storedName);
        if (!Files.exists(snapshotPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(snapshotPath));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read stored JSON snapshot: " + storedName, exception);
        }
    }

    @Override
    public void save(ImportedJsonFile importedJsonFile, String jsonContent) {
        try {
            ensureDirectories();
            Files.writeString(historyDirectory().resolve(importedJsonFile.storedName()), jsonContent);

            List<ImportedJsonFile> entries = findAll().stream()
                    .filter(existing -> !existing.storedName().equals(importedJsonFile.storedName()))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            entries.add(importedJsonFile);
            entries.sort(Comparator.comparing(ImportedJsonFile::importedAt));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath().toFile(), entries);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist JSON history snapshot.", exception);
        }
    }

    @Override
    public void deleteByStoredName(String storedName) {
        try {
            Files.deleteIfExists(historyDirectory().resolve(storedName));
            List<ImportedJsonFile> entries = findAll().stream()
                    .filter(existing -> !existing.storedName().equals(storedName))
                    .toList();
            ensureDirectories();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath().toFile(), entries);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete JSON history snapshot: " + storedName, exception);
        }
    }

    private void ensureDirectories() throws IOException {
        Files.createDirectories(historyDirectory());
        Files.createDirectories(rootDirectory());
    }

    private Path rootDirectory() {
        return appDataProperties.getRootDirectory().toAbsolutePath().normalize();
    }

    private Path historyDirectory() {
        return rootDirectory().resolve(appDataProperties.getHistoryDirectoryName());
    }

    private Path metadataPath() {
        return rootDirectory().resolve(appDataProperties.getMetadataFileName());
    }
}
