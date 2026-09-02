package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.ports.out.CandidateProfileRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class JsonCandidateProfileRepository implements CandidateProfileRepository {

    private final Path filePath;
    private CandidateProfile current;

    public JsonCandidateProfileRepository() {
        this(DataDirectoryResolver.resolveFilePath("profile.json"));
    }

    public JsonCandidateProfileRepository(Path filePath) {
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
            this.current = JsonStorageUtils.readJson(filePath, CandidateProfile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load candidate profile from " + filePath, e);
        }
    }

    @Override
    public synchronized Optional<CandidateProfile> findDefault() {
        return Optional.ofNullable(current);
    }

    @Override
    public synchronized void save(CandidateProfile profile) {
        if (profile != null) {
            this.current = profile;
            try {
                JsonStorageUtils.writeJsonAtomic(filePath, profile);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to save candidate profile to " + filePath, e);
            }
        }
    }
}
