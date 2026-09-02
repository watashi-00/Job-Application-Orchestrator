package com.watashi.core.service;

import com.watashi.core.domain.tag.CustomTag;
import com.watashi.core.ports.in.ManageCustomTagsUseCase;
import com.watashi.core.ports.out.CustomTagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junit.framework.TestCase;

public class DefaultManageCustomTagsServiceTest extends TestCase {

    private static class InMemoryCustomTagRepository implements CustomTagRepository {
        private final List<CustomTag> tags = new ArrayList<>();

        public InMemoryCustomTagRepository() {
            tags.add(new CustomTag("tag-1", "Java 21", "LANGUAGES_FRAMEWORKS", "#2563eb", false));
        }

        public List<CustomTag> findAll() {
            return new ArrayList<>(tags);
        }

        public Optional<CustomTag> findByName(String name) {
            return tags.stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst();
        }

        public void save(CustomTag tag) {
            delete(tag.id());
            tags.add(tag);
        }

        public void delete(String tagId) {
            tags.removeIf(t -> t.id().equals(tagId));
        }
    }

    public void testGetAllAndAddAndRemoveCustomTag() {
        CustomTagRepository repository = new InMemoryCustomTagRepository();
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
    }

    public void testAddTagValidation() {
        CustomTagRepository repository = new InMemoryCustomTagRepository();
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
    }
}
