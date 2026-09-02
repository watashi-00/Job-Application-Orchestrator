package com.watashi.adapters.out.persistence.json;

import com.watashi.core.ports.out.ResumePdfStorageRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class JsonResumePdfStorageRepository implements ResumePdfStorageRepository {

    private final Path filePath;

    public JsonResumePdfStorageRepository() {
        this(DataDirectoryResolver.resolveDataDirectory().resolve("resume.pdf"));
    }

    public JsonResumePdfStorageRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
    }

    @Override
    public synchronized void savePdf(byte[] pdfBytes) {
        Objects.requireNonNull(pdfBytes, "pdfBytes cannot be null");
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.write(filePath, pdfBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save PDF to " + filePath, e);
        }
    }

    @Override
    public synchronized Optional<byte[]> loadPdf() {
        if (!exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load PDF from " + filePath, e);
        }
    }

    @Override
    public synchronized boolean exists() {
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
}
