package com.watashi.core.service;

import com.watashi.adapters.out.persistence.json.JsonCustomTagRepository;
import com.watashi.core.domain.tag.CustomTag;
import com.watashi.core.ports.in.ManageCustomTagsUseCase;
import com.watashi.core.ports.out.CustomTagRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import junit.framework.TestCase;

public class DefaultManageCustomTagsServiceTest extends TestCase {

    public void testGetAllAndAddAndRemoveCustomTag() throws Exception {
        Path tempFile = Files.createTempFile("manage-custom-tags-test", ".json");
        try {
            CustomTagRepository repository = new JsonCustomTagRepository(tempFile);
            ManageCustomTagsUseCase service = new DefaultManageCustomTagsService(repository);

            List<CustomTag> initialTags = service.getAllTags();
            assertFalse(initialTags.isEmpty());

            CustomTag created = service.addCustomTag("GraphQL", "API", "#e10098");
            assertNotNull(created);
            assertEquals("GraphQL", created.name());
            assertEquals("API", created.category());
            assertEquals("#e10098", created.colorHex());
            assertTrue(created.isUserCreated());

            List<CustomTag> updatedTags = service.getAllTags();
            assertTrue(updatedTags.stream().anyMatch(t -> t.name().equals("GraphQL")));

            service.removeCustomTag(created.id());
            List<CustomTag> finalTags = service.getAllTags();
            assertFalse(finalTags.stream().anyMatch(t -> t.name().equals("GraphQL")));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testAddTagValidation() throws Exception {
        Path tempFile = Files.createTempFile("manage-custom-tags-validation", ".json");
        try {
            CustomTagRepository repository = new JsonCustomTagRepository(tempFile);
            ManageCustomTagsUseCase service = new DefaultManageCustomTagsService(repository);

            try {
                service.addCustomTag(null, "API", "#e10098");
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
            }

            try {
                service.addCustomTag("  ", "API", "#e10098");
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
