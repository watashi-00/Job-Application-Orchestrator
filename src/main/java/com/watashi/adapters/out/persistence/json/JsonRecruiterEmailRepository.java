package com.watashi.adapters.out.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.ports.out.RecruiterEmailRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class JsonRecruiterEmailRepository implements RecruiterEmailRepository {

    private final Path filePath;
    private final List<RecruiterEmailMessage> store = new CopyOnWriteArrayList<>();

    public JsonRecruiterEmailRepository() {
        this(DataDirectoryResolver.resolveDataDirectory().resolve("emails-inbox.json"));
    }

    public JsonRecruiterEmailRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        store.clear();
        if (!Files.exists(filePath) || isFileEmpty(filePath)) {
            return;
        }
        try {
            List<RecruiterEmailMessage> messages =
                    JsonStorageUtils.readJson(filePath, new TypeReference<List<RecruiterEmailMessage>>() {});
            if (messages != null) {
                store.addAll(messages);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load recruiter emails from " + filePath, e);
        }
    }

    private boolean isFileEmpty(Path path) {
        try {
            return Files.size(path) == 0;
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public synchronized List<RecruiterEmailMessage> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store));
    }

    @Override
    public synchronized Optional<RecruiterEmailMessage> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return store.stream().filter(msg -> msg.id().equals(id)).findFirst();
    }

    @Override
    public synchronized void save(RecruiterEmailMessage message) {
        Objects.requireNonNull(message, "message cannot be null");
        store.removeIf(msg -> msg.id().equals(message.id()));
        store.add(message);
        writeToFile();
    }

    private void writeToFile() {
        try {
            JsonStorageUtils.writeJsonAtomic(filePath, new ArrayList<>(store));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save recruiter emails to " + filePath, e);
        }
    }
}
