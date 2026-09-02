package com.watashi.adapters.out.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.out.JobApplicationRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class JsonJobApplicationRepository implements JobApplicationRepository {

    private final Path filePath;
    private final Map<String, JobStatus> store = new ConcurrentHashMap<>();

    public JsonJobApplicationRepository() {
        this(DataDirectoryResolver.resolveDataDirectory().resolve("applications-history.json"));
    }

    public JsonJobApplicationRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        if (!Files.exists(filePath)) {
            return;
        }
        try {
            if (Files.size(filePath) == 0) {
                return;
            }
            Map<String, JobStatus> data =
                    JsonStorageUtils.readJson(filePath, new TypeReference<Map<String, JobStatus>>() {});
            if (data != null) {
                store.putAll(data);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load applications history from " + filePath, e);
        }
    }

    @Override
    public synchronized Map<String, JobStatus> loadHistory() {
        return new HashMap<>(store);
    }

    @Override
    public synchronized void saveHistory(Map<String, JobStatus> history) {
        Objects.requireNonNull(history, "history cannot be null");
        store.clear();
        store.putAll(history);
        writeToFile();
    }

    @Override
    public synchronized void updateJobStatus(String jobId, JobStatus status) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        store.put(jobId, status);
        writeToFile();
    }

    private void writeToFile() {
        try {
            JsonStorageUtils.writeJsonAtomic(filePath, new HashMap<>(store));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write applications history to " + filePath, e);
        }
    }
}
