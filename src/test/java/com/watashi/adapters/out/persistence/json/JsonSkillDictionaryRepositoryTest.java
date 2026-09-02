package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.discovery.SkillDictionary;
import java.nio.file.Files;
import java.nio.file.Path;
import junit.framework.TestCase;

public class JsonSkillDictionaryRepositoryTest extends TestCase {

    public void testPersistence() throws Exception {
        Path tempFile = Files.createTempFile("skills-dict-test", ".json");
        try {
            JsonSkillDictionaryRepository repo = new JsonSkillDictionaryRepository(tempFile);
            SkillDictionary dict = repo.load();

            dict.addAlias("golang", "Go");
            repo.save(dict);

            JsonSkillDictionaryRepository repo2 = new JsonSkillDictionaryRepository(tempFile);
            assertEquals("Go", repo2.load().resolveCanonical("golang"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
