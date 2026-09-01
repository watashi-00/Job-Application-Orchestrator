package com.watashi.adapters.out.persistence.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class JsonStorageUtils {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private JsonStorageUtils() {
        // Utility class
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    public static void writeJsonAtomic(Path path, Object data) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(data, "data must not be null");

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tempFile = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try {
            OBJECT_MAPPER.writeValue(tempFile.toFile(), data);
            try {
                Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public static <T> T readJson(Path path, Class<T> clazz) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");

        if (!Files.exists(path)) {
            return null;
        }

        return OBJECT_MAPPER.readValue(path.toFile(), clazz);
    }
}
