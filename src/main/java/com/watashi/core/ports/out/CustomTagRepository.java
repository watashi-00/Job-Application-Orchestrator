package com.watashi.core.ports.out;

import com.watashi.core.domain.tag.CustomTag;
import java.util.List;
import java.util.Optional;

public interface CustomTagRepository {

    List<CustomTag> findAll();

    Optional<CustomTag> findByName(String name);

    void save(CustomTag tag);

    void delete(String tagId);
}
