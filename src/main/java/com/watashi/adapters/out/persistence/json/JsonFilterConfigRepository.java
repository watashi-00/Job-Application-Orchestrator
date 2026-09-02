package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.ports.out.FilterConfigRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class JsonFilterConfigRepository implements FilterConfigRepository {

    private final Path filePath;
    private FilterConfiguration current;

    public JsonFilterConfigRepository() {
        this(DataDirectoryResolver.resolveFilePath("filters.json"));
    }

    public JsonFilterConfigRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        if (!Files.exists(filePath)) {
            this.current = FilterConfiguration.defaultConfig();
            return;
        }
        try {
            if (Files.size(filePath) == 0) {
                this.current = FilterConfiguration.defaultConfig();
                return;
            }
            FilterConfiguration config = JsonStorageUtils.readJson(filePath, FilterConfiguration.class);
            this.current = config != null ? config : FilterConfiguration.defaultConfig();
        } catch (IOException e) {
            this.current = FilterConfiguration.defaultConfig();
        }
    }

    @Override
    public synchronized FilterConfiguration load() {
        return current != null ? current : FilterConfiguration.defaultConfig();
    }

    @Override
    public synchronized void save(FilterConfiguration config) {
        if (config != null) {
            this.current = config;
            try {
                JsonStorageUtils.writeJsonAtomic(filePath, config);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to save filter configuration to " + filePath, e);
            }
        }
    }
}
