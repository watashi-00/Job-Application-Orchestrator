package com.watashi.core.service;

import com.watashi.core.domain.tag.CustomTag;
import com.watashi.core.ports.in.ManageCustomTagsUseCase;
import com.watashi.core.ports.out.CustomTagRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DefaultManageCustomTagsService implements ManageCustomTagsUseCase {

    private final CustomTagRepository tagRepository;

    public DefaultManageCustomTagsService(CustomTagRepository tagRepository) {
        this.tagRepository = Objects.requireNonNull(tagRepository, "tagRepository cannot be null");
    }

    @Override
    public List<CustomTag> getAllTags() {
        return tagRepository.findAll();
    }

    @Override
    public CustomTag addCustomTag(String name, String category, String colorHex) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be null or blank");
        }
        String trimmedName = name.trim();
        String tagId = "user-tag-" + UUID.randomUUID().toString().substring(0, 8);
        CustomTag customTag = new CustomTag(tagId, trimmedName, category, colorHex, true);
        tagRepository.save(customTag);
        return customTag;
    }

    @Override
    public void removeCustomTag(String tagId) {
        if (tagId == null || tagId.isBlank()) {
            return;
        }
        tagRepository.delete(tagId);
    }
}
