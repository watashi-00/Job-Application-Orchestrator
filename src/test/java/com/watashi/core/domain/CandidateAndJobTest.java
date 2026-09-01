package com.watashi.core.domain;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.*;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import java.math.BigDecimal;
import java.util.Set;
import junit.framework.TestCase;

public class CandidateAndJobTest extends TestCase {

    public void testCandidateProfileCreation() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        CandidateProfile profile = new CandidateProfile(
                "cand-1",
                "Senior Backend Engineer",
                "Experienced Java Dev",
                Set.of(java),
                Set.of(SeniorityLevel.SENIOR, SeniorityLevel.LEAD),
                Set.of(WorkMode.REMOTE, WorkMode.HYBRID),
                new SalaryRange(new BigDecimal("15000"), new BigDecimal("20000"), "BRL"),
                Set.of("Brazil", "Remote"));
        assertEquals("cand-1", profile.id());
        assertTrue(profile.hasSkillNamed("java"));
    }

    public void testJobOpportunityCreation() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
                "job-101",
                "Java Developer",
                "TechCorp",
                "We need Java dev",
                Set.of(java),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                new SalaryRange(new BigDecimal("16000"), new BigDecimal("18000"), "BRL"),
                "https://example.com/jobs/101",
                JobStatus.DISCOVERED);
        assertEquals("job-101", job.id());
        assertEquals(JobStatus.DISCOVERED, job.status());
    }
}
