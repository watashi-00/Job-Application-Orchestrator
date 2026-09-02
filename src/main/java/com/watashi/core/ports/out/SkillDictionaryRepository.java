package com.watashi.core.ports.out;

import com.watashi.core.domain.discovery.SkillDictionary;

public interface SkillDictionaryRepository {
    SkillDictionary load();

    void save(SkillDictionary dictionary);
}
