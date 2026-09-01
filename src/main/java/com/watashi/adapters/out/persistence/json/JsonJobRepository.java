package com.watashi.adapters.out.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.ports.out.JobRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JsonJobRepository implements JobRepository {

    private final Path filePath;
    private final Map<String, JobOpportunity> store = new ConcurrentHashMap<>();

    public JsonJobRepository() {
        this(Paths.get("data", "jobs.json"));
    }

    public JsonJobRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        loadFromFile();
    }

    private void loadFromFile() {
        if (!Files.exists(filePath)) {
            return;
        }
        try {
            if (Files.size(filePath) == 0) {
                return;
            }
            ObjectMapper mapper = JsonStorageUtils.createObjectMapper();
            List<JobOpportunity> jobs =
                    mapper.readValue(filePath.toFile(), new TypeReference<List<JobOpportunity>>() {});
            if (jobs != null) {
                for (JobOpportunity job : jobs) {
                    if (job != null && job.id() != null) {
                        store.put(job.id(), job);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load jobs from " + filePath, e);
        }
    }

    @Override
    public synchronized void save(JobOpportunity job) {
        if (job == null || job.id() == null) {
            return;
        }
        store.put(job.id(), job);
        writeToFile();
    }

    private void writeToFile() {
        try {
            JsonStorageUtils.writeJsonAtomic(filePath, new ArrayList<>(store.values()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write jobs to " + filePath, e);
        }
    }

    @Override
    public List<JobOpportunity> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public Optional<JobOpportunity> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(id));
    }
}
