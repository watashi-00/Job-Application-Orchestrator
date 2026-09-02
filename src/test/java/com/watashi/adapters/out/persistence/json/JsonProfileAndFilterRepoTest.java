package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import junit.framework.TestCase;

public class JsonProfileAndFilterRepoTest extends TestCase {

    public void testProfileAndFilterPersistence() throws Exception {
        Path profileFile = Files.createTempFile("profile-test", ".json");
        Path filterFile = Files.createTempFile("filter-test", ".json");

        try {
            CandidateProfileRepository profileRepo = new JsonCandidateProfileRepository(profileFile);
            CandidateProfile profile =
                    new CandidateProfile("c1", "Senior Dev", "Summary", Set.of(), Set.of(), Set.of(), null, Set.of());
            profileRepo.save(profile);

            CandidateProfileRepository profileRepo2 = new JsonCandidateProfileRepository(profileFile);
            assertEquals("c1", profileRepo2.findDefault().orElseThrow().id());

            FilterConfigRepository filterRepo = new JsonFilterConfigRepository(filterFile);
            FilterConfiguration config = FilterConfiguration.defaultConfig();
            filterRepo.save(config);

            FilterConfigRepository filterRepo2 = new JsonFilterConfigRepository(filterFile);
            assertEquals(75.0, filterRepo2.load().minimumScoreThreshold(), 0.01);
        } finally {
            Files.deleteIfExists(profileFile);
            Files.deleteIfExists(filterFile);
        }
    }
}
