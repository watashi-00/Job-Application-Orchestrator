package com.watashi.adapters.out.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.watashi.core.domain.discovery.SkillDictionary;
import com.watashi.core.ports.out.SkillDictionaryRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public class JsonSkillDictionaryRepository implements SkillDictionaryRepository {

    private final Path filePath;

    public JsonSkillDictionaryRepository() {
        this(DataDirectoryResolver.resolveDataDirectory().resolve("skills-dictionary.json"));
    }

    public JsonSkillDictionaryRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
    }

    public SkillDictionary load() {
        try {
            Map<String, String> map = JsonStorageUtils.readJson(filePath, new TypeReference<Map<String, String>>() {});
            if (map == null || map.isEmpty()) {
                return SkillDictionary.defaultDictionary();
            }
            SkillDictionary dictionary = SkillDictionary.defaultDictionary();
            map.forEach(dictionary::addAlias);
            return dictionary;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load skill dictionary from " + filePath, e);
        }
    }

    public void save(SkillDictionary dictionary) {
        if (dictionary == null) {
            return;
        }
        try {
            JsonStorageUtils.writeJsonAtomic(filePath, dictionary.getAliasMap());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save skill dictionary to " + filePath, e);
        }
    }
}
