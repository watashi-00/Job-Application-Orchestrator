package com.watashi.adapters.out.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.watashi.core.domain.application.CandidateCredential;
import com.watashi.core.ports.out.CandidateCredentialsRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JsonCandidateCredentialsRepository implements CandidateCredentialsRepository {

    private final Path filePath;
    private final Map<String, CandidateCredential> store = new ConcurrentHashMap<>();

    public JsonCandidateCredentialsRepository() {
        this(DataDirectoryResolver.resolveDataDirectory().resolve("credentials-store.json"));
    }

    public JsonCandidateCredentialsRepository(Path filePath) {
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
            Map<String, CandidateCredential> data =
                    JsonStorageUtils.readJson(filePath, new TypeReference<Map<String, CandidateCredential>>() {});
            if (data != null) {
                store.putAll(data);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load credentials from " + filePath, e);
        }
    }

    @Override
    public synchronized Optional<CandidateCredential> findByDomain(String domain) {
        if (domain == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(domain));
    }

    @Override
    public synchronized void saveCredential(CandidateCredential credential) {
        Objects.requireNonNull(credential, "credential cannot be null");
        store.put(credential.domain(), credential);
        writeToFile();
    }

    @Override
    public synchronized Map<String, CandidateCredential> findAll() {
        return new HashMap<>(store);
    }

    private void writeToFile() {
        try {
            JsonStorageUtils.writeJsonAtomic(filePath, new HashMap<>(store));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write credentials to " + filePath, e);
        }
    }
}
