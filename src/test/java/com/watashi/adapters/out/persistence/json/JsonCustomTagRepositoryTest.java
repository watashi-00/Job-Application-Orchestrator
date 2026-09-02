package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.tag.CustomTag;
import com.watashi.core.ports.out.CustomTagRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import junit.framework.TestCase;

public class JsonCustomTagRepositoryTest extends TestCase {

    public void testDefaultTagsLoadedWhenFileMissingOrEmpty() throws Exception {
        Path tempFile = Files.createTempFile("custom-tags-test", ".json");
        try {
            CustomTagRepository repo = new JsonCustomTagRepository(tempFile);
            List<CustomTag> tags = repo.findAll();
            assertFalse(tags.isEmpty());
            assertTrue(tags.stream().anyMatch(t -> t.name().equals("Java 21")));
            assertTrue(tags.stream().anyMatch(t -> t.name().equals("Spring Boot")));
            assertTrue(tags.stream().anyMatch(t -> t.name().equals("PostgreSQL")));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testSaveFindByNameAndDelete() throws Exception {
        Path tempFile = Files.createTempFile("custom-tags-save-test", ".json");
        try {
            CustomTagRepository repo1 = new JsonCustomTagRepository(tempFile);
            CustomTag newTag = new CustomTag("rust-lang", "Rust", "LANGUAGES_FRAMEWORKS", "#ce412b", true);
            repo1.save(newTag);

            Optional<CustomTag> found = repo1.findByName("Rust");
            assertTrue(found.isPresent());
            assertEquals("rust-lang", found.get().id());
            assertEquals("Rust", found.get().name());
            assertEquals("LANGUAGES_FRAMEWORKS", found.get().category());
            assertEquals("#ce412b", found.get().colorHex());
            assertTrue(found.get().isUserCreated());

            // Re-instantiate repo to verify persistence
            CustomTagRepository repo2 = new JsonCustomTagRepository(tempFile);
            Optional<CustomTag> found2 = repo2.findByName("rust");
            assertTrue(found2.isPresent());

            // Delete tag
            repo2.delete("rust-lang");
            assertFalse(repo2.findByName("Rust").isPresent());

            CustomTagRepository repo3 = new JsonCustomTagRepository(tempFile);
            assertFalse(repo3.findByName("Rust").isPresent());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
