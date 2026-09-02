package com.watashi.adapters.out.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.watashi.core.domain.tag.CustomTag;
import com.watashi.core.ports.out.CustomTagRepository;
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

public class JsonCustomTagRepository implements CustomTagRepository {

    private final Path filePath;
    private final List<CustomTag> store = new CopyOnWriteArrayList<>();

    public JsonCustomTagRepository() {
        this(DataDirectoryResolver.resolveDataDirectory().resolve("custom-tags.json"));
    }

    public JsonCustomTagRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        store.clear();
        if (!Files.exists(filePath) || isFileEmpty(filePath)) {
            store.addAll(defaultTags());
            writeToFile();
            return;
        }
        try {
            List<CustomTag> tags = JsonStorageUtils.readJson(filePath, new TypeReference<List<CustomTag>>() {});
            if (tags == null || tags.isEmpty()) {
                store.addAll(defaultTags());
                writeToFile();
            } else {
                store.addAll(tags);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load custom tags from " + filePath, e);
        }
    }

    private boolean isFileEmpty(Path path) {
        try {
            return Files.size(path) == 0;
        } catch (IOException e) {
            return true;
        }
    }

    private List<CustomTag> defaultTags() {
        return List.of(
                new CustomTag("java-21", "Java 21", "LANGUAGES_FRAMEWORKS", "#007acc", false),
                new CustomTag("spring-boot", "Spring Boot", "LANGUAGES_FRAMEWORKS", "#6db33f", false),
                new CustomTag("postgresql", "PostgreSQL", "DATABASE", "#336791", false),
                new CustomTag("docker", "Docker", "INFRASTRUCTURE", "#2496ed", false),
                new CustomTag("virtual-threads", "Virtual Threads", "LANGUAGES_FRAMEWORKS", "#f0932b", false),
                new CustomTag("kubernetes", "Kubernetes", "INFRASTRUCTURE", "#326ce5", false),
                new CustomTag("microservices", "Microservices", "ARCHITECTURE", "#8e44ad", false),
                new CustomTag("remote", "Remote", "LOCATION", "#27ae60", false),
                new CustomTag("latam", "LATAM", "LOCATION", "#e67e22", false));
    }

    @Override
    public synchronized List<CustomTag> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store));
    }

    @Override
    public synchronized Optional<CustomTag> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        return store.stream()
                .filter(tag -> tag.name().equalsIgnoreCase(trimmed))
                .findFirst();
    }

    @Override
    public synchronized void save(CustomTag tag) {
        Objects.requireNonNull(tag, "tag cannot be null");
        store.removeIf(t -> t.id().equalsIgnoreCase(tag.id()) || t.name().equalsIgnoreCase(tag.name()));
        store.add(tag);
        writeToFile();
    }

    @Override
    public synchronized void delete(String tagId) {
        if (tagId == null) {
            return;
        }
        boolean removed = store.removeIf(t -> t.id().equalsIgnoreCase(tagId));
        if (removed) {
            writeToFile();
        }
    }

    private void writeToFile() {
        try {
            JsonStorageUtils.writeJsonAtomic(filePath, new ArrayList<>(store));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save custom tags to " + filePath, e);
        }
    }
}
