package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.ports.out.FilterConfigRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class JsonFilterConfigRepository implements FilterConfigRepository {

    private final Path filePath;
    private FilterConfiguration current;

    public JsonFilterConfigRepository() {
        this(Paths.get("data", "filters.json"));
    }

    public JsonFilterConfigRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        loadFromFile();
    }

    private void loadFromFile() {
        if (!Files.exists(filePath)) {
            this.current = FilterConfiguration.defaultConfig();
            return;
        }
        try {
            if (Files.size(filePath) == 0) {
                this.current = FilterConfiguration.defaultConfig();
                return;
            }
            FilterConfiguration loaded = JsonStorageUtils.readJson(filePath, FilterConfiguration.class);
            this.current = loaded != null ? loaded : FilterConfiguration.defaultConfig();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load filter config from " + filePath, e);
        }
    }

    @Override
    public synchronized FilterConfiguration load() {
        return current != null ? current : FilterConfiguration.defaultConfig();
    }

    @Override
    public synchronized void save(FilterConfiguration config) {
        this.current = config;
        if (config != null) {
            try {
                JsonStorageUtils.writeJsonAtomic(filePath, config);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write filter config to " + filePath, e);
            }
        }
    }
}
