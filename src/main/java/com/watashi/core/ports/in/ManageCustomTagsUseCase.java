package com.watashi.core.ports.in;

import com.watashi.core.domain.tag.CustomTag;
import java.util.List;

public interface ManageCustomTagsUseCase {

    List<CustomTag> getAllTags();

    CustomTag addCustomTag(String name, String category, String colorHex);

    void removeCustomTag(String tagId);
}
