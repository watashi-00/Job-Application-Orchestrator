# Core Domain & Matching Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Core Domain Models, Filter Configuration, and pure functional Matching Engine (MVP 0.0.1) for Job Application Orchestrator following Hexagonal Architecture in Java 21.

**Architecture:** Hexagonal Architecture (Clean Architecture) separating `core/domain`, `core/ports/in`, and `core/ports/out`. Models are implemented as immutable Java 21 `record`s and `enum`s. The `MatchingEngine` is a pure domain service evaluating job compatibility scores (0-100%) with granular breakdown, missing skills, and explicit conflict detection.

**Tech Stack:** Java 21, JUnit 3/4/5 (JUnit 3.8.1 currently in pom.xml, upgraded or compatible), Maven 3.9+, Spotless (Palantir Java Format).

## Global Constraints

- JDK: 21 (source and target = 21 in pom.xml)
- Package root: `com.watashi`
- Formatting: `mvn spotless:apply` / `mvn spotless:check`
- Code style: Immutable records, no null pointers, explicit null checks in record compact constructors.

---

### Task 1: Common Domain Value Objects

**Files:**
- Create: `src/main/java/com/watashi/core/domain/common/SeniorityLevel.java`
- Create: `src/main/java/com/watashi/core/domain/common/WorkMode.java`
- Create: `src/main/java/com/watashi/core/domain/common/SkillCategory.java`
- Create: `src/main/java/com/watashi/core/domain/common/Skill.java`
- Create: `src/main/java/com/watashi/core/domain/common/SalaryRange.java`
- Create: `src/test/java/com/watashi/core/domain/common/ValueObjectsTest.java`

**Interfaces:**
- Consumes: None
- Produces: `SeniorityLevel`, `WorkMode`, `SkillCategory`, `Skill(String name, SkillCategory category, int yearsExperience)`, `SalaryRange(BigDecimal min, BigDecimal max, String currency)`

- [ ] **Step 1: Write the failing unit test for value objects**

```java
package com.watashi.core.domain.common;

import junit.framework.TestCase;
import java.math.BigDecimal;

public class ValueObjectsTest extends TestCase {

    public void testSkillCreationAndEquality() {
        Skill s1 = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        Skill s2 = new Skill("java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        assertEquals("java", s1.nameLower());
        assertTrue(s1.matchesName("JAVA"));
    }

    public void testSalaryRangeCoversMinimum() {
        SalaryRange range = new SalaryRange(new BigDecimal("10000"), new BigDecimal("15000"), "BRL");
        assertTrue(range.coversMinimum(new BigDecimal("12000")));
        assertFalse(range.coversMinimum(new BigDecimal("18000")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ValueObjectsTest`
Expected: FAIL due to missing classes.

- [ ] **Step 3: Implement Value Objects**

`SeniorityLevel.java`:
```java
package com.watashi.core.domain.common;

public enum SeniorityLevel {
    INTERN(1),
    JUNIOR(2),
    MID(3),
    SENIOR(4),
    LEAD(5),
    PRINCIPAL(6);

    private final int level;

    SeniorityLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public int distanceTo(SeniorityLevel other) {
        return Math.abs(this.level - other.level);
    }
}
```

`WorkMode.java`:
```java
package com.watashi.core.domain.common;

public enum WorkMode {
    REMOTE,
    HYBRID,
    ONSITE
}
```

`SkillCategory.java`:
```java
package com.watashi.core.domain.common;

public enum SkillCategory {
    LANGUAGES_FRAMEWORKS,
    DATABASE,
    DEVOPS_CLOUD,
    ARCHITECTURE_DESIGN,
    METHODOLOGY_SOFT_SKILLS,
    OTHER
}
```

`Skill.java`:
```java
package com.watashi.core.domain.common;

import java.util.Objects;

public record Skill(String name, SkillCategory category, int yearsExperience) {
    public Skill {
        Objects.requireNonNull(name, "Skill name cannot be null");
        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Skill name cannot be empty");
        }
        if (category == null) {
            category = SkillCategory.OTHER;
        }
    }

    public String nameLower() {
        return name.toLowerCase();
    }

    public boolean matchesName(String otherName) {
        return otherName != null && nameLower().equalsIgnoreCase(otherName.trim());
    }
}
```

`SalaryRange.java`:
```java
package com.watashi.core.domain.common;

import java.math.BigDecimal;
import java.util.Objects;

public record SalaryRange(BigDecimal min, BigDecimal max, String currency) {
    public SalaryRange {
        if (currency == null) {
            currency = "USD";
        }
    }

    public boolean coversMinimum(BigDecimal expectedMin) {
        if (expectedMin == null) return true;
        if (max == null) return true;
        return max.compareTo(expectedMin) >= 0;
    }
}
```

- [ ] **Step 4: Run test to verify it passes & check formatting**

Run: `mvn test -Dtest=ValueObjectsTest`
Expected: PASS (1 test)

- [ ] **Step 5: Format and commit**

```bash
mvn spotless:apply
git add src/main/java/com/watashi/core/domain/common/ src/test/java/com/watashi/core/domain/common/
git commit -m "feat(core): add common domain value objects (Seniority, WorkMode, Skill, SalaryRange)"
```

---

### Task 2: Candidate Profile & Job Opportunity Domain Entities

**Files:**
- Create: `src/main/java/com/watashi/core/domain/job/JobStatus.java`
- Create: `src/main/java/com/watashi/core/domain/candidate/CandidateProfile.java`
- Create: `src/main/java/com/watashi/core/domain/job/JobOpportunity.java`
- Create: `src/test/java/com/watashi/core/domain/CandidateAndJobTest.java`

**Interfaces:**
- Consumes: `Skill`, `SeniorityLevel`, `WorkMode`, `SalaryRange`
- Produces: `JobStatus`, `CandidateProfile`, `JobOpportunity`

- [ ] **Step 1: Write the failing unit test**

```java
package com.watashi.core.domain;

import junit.framework.TestCase;
import com.watashi.core.domain.common.*;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import java.math.BigDecimal;
import java.util.Set;

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
            Set.of("Brazil", "Remote")
        );
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
            JobStatus.DISCOVERED
        );
        assertEquals("job-101", job.id());
        assertEquals(JobStatus.DISCOVERED, job.status());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=CandidateAndJobTest`
Expected: FAIL due to missing `JobStatus`, `CandidateProfile`, `JobOpportunity`.

- [ ] **Step 3: Implement CandidateProfile and JobOpportunity**

`JobStatus.java`:
```java
package com.watashi.core.domain.job;

public enum JobStatus {
    DISCOVERED,
    EVALUATED,
    APPLIED,
    INTERVIEWING,
    REJECTED,
    OFFER
}
```

`CandidateProfile.java`:
```java
package com.watashi.core.domain.candidate;

import com.watashi.core.domain.common.*;
import java.util.Objects;
import java.util.Set;

public record CandidateProfile(
    String id,
    String title,
    String summary,
    Set<Skill> skills,
    Set<SeniorityLevel> targetSeniorities,
    Set<WorkMode> preferredWorkModes,
    SalaryRange desiredSalary,
    Set<String> preferredLocations
) {
    public CandidateProfile {
        Objects.requireNonNull(id, "Candidate ID cannot be null");
        skills = skills == null ? Set.of() : Set.copyOf(skills);
        targetSeniorities = targetSeniorities == null ? Set.of() : Set.copyOf(targetSeniorities);
        preferredWorkModes = preferredWorkModes == null ? Set.of() : Set.copyOf(preferredWorkModes);
        preferredLocations = preferredLocations == null ? Set.of() : Set.copyOf(preferredLocations);
    }

    public boolean hasSkillNamed(String skillName) {
        if (skillName == null) return false;
        return skills.stream().anyMatch(s -> s.matchesName(skillName));
    }
}
```

`JobOpportunity.java`:
```java
package com.watashi.core.domain.job;

import com.watashi.core.domain.common.*;
import java.util.Objects;
import java.util.Set;

public record JobOpportunity(
    String id,
    String title,
    String company,
    String description,
    Set<Skill> requiredSkills,
    Set<Skill> optionalSkills,
    SeniorityLevel seniorityLevel,
    WorkMode workMode,
    String location,
    SalaryRange salaryRange,
    String sourceUrl,
    JobStatus status
) {
    public JobOpportunity {
        Objects.requireNonNull(id, "Job ID cannot be null");
        Objects.requireNonNull(title, "Job title cannot be null");
        requiredSkills = requiredSkills == null ? Set.of() : Set.copyOf(requiredSkills);
        optionalSkills = optionalSkills == null ? Set.of() : Set.copyOf(optionalSkills);
        status = status == null ? JobStatus.DISCOVERED : status;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=CandidateAndJobTest`
Expected: PASS

- [ ] **Step 5: Format and commit**

```bash
mvn spotless:apply
git add src/main/java/com/watashi/core/domain/candidate/ src/main/java/com/watashi/core/domain/job/ src/test/java/com/watashi/core/domain/CandidateAndJobTest.java
git commit -m "feat(core): add CandidateProfile and JobOpportunity domain entities"
```

---

### Task 3: Filter Configuration & Match Result Models

**Files:**
- Create: `src/main/java/com/watashi/core/domain/matching/FilterConfiguration.java`
- Create: `src/main/java/com/watashi/core/domain/matching/ScoreBreakdown.java`
- Create: `src/main/java/com/watashi/core/domain/matching/MatchStatus.java`
- Create: `src/main/java/com/watashi/core/domain/matching/MatchResult.java`
- Create: `src/test/java/com/watashi/core/domain/matching/MatchResultTest.java`

**Interfaces:**
- Consumes: `Skill`
- Produces: `FilterConfiguration`, `ScoreBreakdown`, `MatchStatus`, `MatchResult`

- [ ] **Step 1: Write failing unit test**

```java
package com.watashi.core.domain.matching;

import junit.framework.TestCase;
import com.watashi.core.domain.common.*;
import java.util.List;
import java.util.Set;

public class MatchResultTest extends TestCase {

    public void testFilterConfigurationDefaults() {
        FilterConfiguration config = FilterConfiguration.defaultConfig();
        assertEquals(0.50, config.techWeight(), 0.001);
        assertEquals(75.0, config.minimumScoreThreshold(), 0.001);
        assertFalse(config.strictRequiredSkills());
    }

    public void testMatchResultCreation() {
        ScoreBreakdown breakdown = new ScoreBreakdown(100.0, 100.0, 100.0, 100.0);
        MatchResult result = new MatchResult(
            "job-1",
            100.0,
            breakdown,
            Set.of(new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5)),
            Set.of(),
            Set.of(),
            List.of(),
            MatchStatus.RECOMMENDED
        );
        assertEquals(100.0, result.overallScore());
        assertEquals(MatchStatus.RECOMMENDED, result.status());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=MatchResultTest`
Expected: FAIL due to missing matching domain classes.

- [ ] **Step 3: Implement Matching Models**

`FilterConfiguration.java`:
```java
package com.watashi.core.domain.matching;

public record FilterConfiguration(
    double techWeight,
    double seniorityWeight,
    double workModeWeight,
    double salaryWeight,
    double minimumScoreThreshold,
    boolean strictRequiredSkills
) {
    public static FilterConfiguration defaultConfig() {
        return new FilterConfiguration(0.50, 0.20, 0.15, 0.15, 75.0, false);
    }
}
```

`ScoreBreakdown.java`:
```java
package com.watashi.core.domain.matching;

public record ScoreBreakdown(
    double techScore,
    double seniorityScore,
    double workModeScore,
    double salaryScore
) {}
```

`MatchStatus.java`:
```java
package com.watashi.core.domain.matching;

public enum MatchStatus {
    RECOMMENDED,
    CONDITIONAL,
    REJECTED
}
```

`MatchResult.java`:
```java
package com.watashi.core.domain.matching;

import com.watashi.core.domain.common.Skill;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record MatchResult(
    String jobId,
    double overallScore,
    ScoreBreakdown breakdown,
    Set<Skill> matchedSkills,
    Set<Skill> missingRequiredSkills,
    Set<Skill> missingOptionalSkills,
    List<String> conflicts,
    MatchStatus status
) {
    public MatchResult {
        Objects.requireNonNull(jobId, "Job ID cannot be null");
        matchedSkills = matchedSkills == null ? Set.of() : Set.copyOf(matchedSkills);
        missingRequiredSkills = missingRequiredSkills == null ? Set.of() : Set.copyOf(missingRequiredSkills);
        missingOptionalSkills = missingOptionalSkills == null ? Set.of() : Set.copyOf(missingOptionalSkills);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        status = status == null ? MatchStatus.REJECTED : status;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=MatchResultTest`
Expected: PASS

- [ ] **Step 5: Format and commit**

```bash
mvn spotless:apply
git add src/main/java/com/watashi/core/domain/matching/ src/test/java/com/watashi/core/domain/matching/MatchResultTest.java
git commit -m "feat(core): add FilterConfiguration and MatchResult matching domain models"
```

---

### Task 4: Pure Functional Matching Engine

**Files:**
- Create: `src/main/java/com/watashi/core/domain/matching/MatchingEngine.java`
- Create: `src/test/java/com/watashi/core/domain/matching/MatchingEngineTest.java`

**Interfaces:**
- Consumes: `CandidateProfile`, `JobOpportunity`, `FilterConfiguration`
- Produces: `MatchResult evaluate(JobOpportunity job, CandidateProfile profile, FilterConfiguration config)`

- [ ] **Step 1: Write comprehensive failing unit tests for MatchingEngine**

```java
package com.watashi.core.domain.matching;

import junit.framework.TestCase;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.*;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import java.math.BigDecimal;
import java.util.Set;

public class MatchingEngineTest extends TestCase {

    private MatchingEngine engine;
    private CandidateProfile candidate;
    private FilterConfiguration defaultConfig;

    protected void setUp() throws Exception {
        engine = new MatchingEngine();
        defaultConfig = FilterConfiguration.defaultConfig();

        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        Skill spring = new Skill("Spring Boot", SkillCategory.LANGUAGES_FRAMEWORKS, 4);

        candidate = new CandidateProfile(
            "cand-1",
            "Backend Engineer",
            "Java Dev",
            Set.of(java, spring),
            Set.of(SeniorityLevel.SENIOR),
            Set.of(WorkMode.REMOTE),
            new SalaryRange(new BigDecimal("10000"), new BigDecimal("15000"), "BRL"),
            Set.of("Remote")
        );
    }

    public void testPerfectMatchScoresHigh() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
            "job-1",
            "Senior Java Dev",
            "CompanyA",
            "Desc",
            Set.of(java),
            Set.of(),
            SeniorityLevel.SENIOR,
            WorkMode.REMOTE,
            "Remote",
            new SalaryRange(new BigDecimal("12000"), new BigDecimal("14000"), "BRL"),
            "http://example.com",
            JobStatus.DISCOVERED
        );

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertEquals(100.0, result.overallScore(), 0.01);
        assertEquals(MatchStatus.RECOMMENDED, result.status());
        assertTrue(result.conflicts().isEmpty());
    }

    public void testStrictRequiredSkillsRejection() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        Skill rust = new Skill("Rust", SkillCategory.LANGUAGES_FRAMEWORKS, 2);

        JobOpportunity job = new JobOpportunity(
            "job-2",
            "Rust & Java Dev",
            "CompanyB",
            "Desc",
            Set.of(java, rust),
            Set.of(),
            SeniorityLevel.SENIOR,
            WorkMode.REMOTE,
            "Remote",
            null,
            "http://example.com",
            JobStatus.DISCOVERED
        );

        FilterConfiguration strictConfig = new FilterConfiguration(0.5, 0.2, 0.15, 0.15, 75.0, true);
        MatchResult result = engine.evaluate(job, candidate, strictConfig);

        assertEquals(MatchStatus.REJECTED, result.status());
        assertFalse(result.conflicts().isEmpty());
    }

    public void testWorkModeConflictRemoteOnlyVsOnsite() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
            "job-3",
            "Java Dev Onsite",
            "CompanyC",
            "Desc",
            Set.of(java),
            Set.of(),
            SeniorityLevel.SENIOR,
            WorkMode.ONSITE,
            "Office",
            null,
            "http://example.com",
            JobStatus.DISCOVERED
        );

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertTrue(result.conflicts().stream().anyMatch(c -> c.contains("ONSITE")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=MatchingEngineTest`
Expected: FAIL due to missing `MatchingEngine`.

- [ ] **Step 3: Implement MatchingEngine**

`MatchingEngine.java`:
```java
package com.watashi.core.domain.matching;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.job.JobOpportunity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MatchingEngine {

    public MatchResult evaluate(JobOpportunity job, CandidateProfile profile, FilterConfiguration config) {
        List<String> conflicts = new ArrayList<>();

        // 1. Technical Skills Score
        Set<Skill> matchedSkills = new HashSet<>();
        Set<Skill> missingRequired = new HashSet<>();
        Set<Skill> missingOptional = new HashSet<>();

        for (Skill req : job.requiredSkills()) {
            if (profile.hasSkillNamed(req.name())) {
                matchedSkills.add(req);
            } else {
                missingRequired.add(req);
            }
        }

        for (Skill opt : job.optionalSkills()) {
            if (profile.hasSkillNamed(opt.name())) {
                matchedSkills.add(opt);
            } else {
                missingOptional.add(opt);
            }
        }

        double reqRatio = job.requiredSkills().isEmpty() ? 1.0 : (double) (job.requiredSkills().size() - missingRequired.size()) / job.requiredSkills().size();
        double optRatio = job.optionalSkills().isEmpty() ? 1.0 : (double) (job.optionalSkills().size() - missingOptional.size()) / job.optionalSkills().size();
        double techScore = (reqRatio * 85.0) + (optRatio * 15.0);

        boolean strictFailed = false;
        if (config.strictRequiredSkills() && !missingRequired.isEmpty()) {
            strictFailed = true;
            String missingNames = missingRequired.stream().map(Skill::name).collect(Collectors.joining(", "));
            conflicts.add("Strict match failed: Missing mandatory required skill(s): " + missingNames);
        }

        // 2. Seniority Score
        double seniorityScore = 100.0;
        if (job.seniorityLevel() != null && !profile.targetSeniorities().isEmpty()) {
            if (profile.targetSeniorities().contains(job.seniorityLevel())) {
                seniorityScore = 100.0;
            } else {
                int minDistance = profile.targetSeniorities().stream()
                    .mapToInt(s -> s.distanceTo(job.seniorityLevel()))
                    .min()
                    .orElse(99);
                if (minDistance == 1) {
                    seniorityScore = 60.0;
                } else {
                    seniorityScore = 0.0;
                    conflicts.add("Seniority mismatch: Job requires " + job.seniorityLevel() + " but candidate target is " + profile.targetSeniorities());
                }
            }
        }

        // 3. Work Mode Score
        double workModeScore = 100.0;
        if (job.workMode() != null && !profile.preferredWorkModes().isEmpty()) {
            if (profile.preferredWorkModes().contains(job.workMode())) {
                workModeScore = 100.0;
            } else if (profile.preferredWorkModes().contains(WorkMode.REMOTE) && job.workMode() == WorkMode.ONSITE) {
                workModeScore = 0.0;
                conflicts.add("Work mode conflict: Job requires ONSITE but candidate prefers REMOTE");
            } else {
                workModeScore = 50.0;
            }
        }

        // 4. Salary Score
        double salaryScore = 100.0;
        if (job.salaryRange() != null && profile.desiredSalary() != null) {
            if (!job.salaryRange().coversMinimum(profile.desiredSalary().min())) {
                salaryScore = 40.0;
                conflicts.add("Salary expectation conflict: Job salary offer is below candidate minimum requirement");
            }
        }

        // Overall Weighted Calculation
        double overallScore = (techScore * config.techWeight())
            + (seniorityScore * config.seniorityWeight())
            + (workModeScore * config.workModeWeight())
            + (salaryScore * config.salaryWeight());

        MatchStatus status;
        if (strictFailed || overallScore < 50.0) {
            status = MatchStatus.REJECTED;
        } else if (overallScore >= config.minimumScoreThreshold()) {
            status = MatchStatus.RECOMMENDED;
        } else {
            status = MatchStatus.CONDITIONAL;
        }

        ScoreBreakdown breakdown = new ScoreBreakdown(techScore, seniorityScore, workModeScore, salaryScore);
        return new MatchResult(
            job.id(),
            overallScore,
            breakdown,
            matchedSkills,
            missingRequired,
            missingOptional,
            conflicts,
            status
        );
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=MatchingEngineTest`
Expected: PASS (3 tests)

- [ ] **Step 5: Format and commit**

```bash
mvn spotless:apply
git add src/main/java/com/watashi/core/domain/matching/MatchingEngine.java src/test/java/com/watashi/core/domain/matching/MatchingEngineTest.java
git commit -m "feat(core): implement pure functional MatchingEngine with weighted score & conflict detection"
```

---

### Task 5: Use Cases & Ports (Inbound/Outbound Interfaces & Service)

**Files:**
- Create: `src/main/java/com/watashi/core/ports/in/AssessJobCompatibilityUseCase.java`
- Create: `src/main/java/com/watashi/core/ports/in/ManageCandidateProfileUseCase.java`
- Create: `src/main/java/com/watashi/core/ports/in/ManageFilterConfigUseCase.java`
- Create: `src/main/java/com/watashi/core/ports/out/CandidateProfileRepository.java`
- Create: `src/main/java/com/watashi/core/ports/out/JobRepository.java`
- Create: `src/main/java/com/watashi/core/ports/out/FilterConfigRepository.java`
- Create: `src/main/java/com/watashi/core/ports/in/DefaultAssessJobCompatibilityService.java`
- Create: `src/test/java/com/watashi/core/ports/in/DefaultAssessJobCompatibilityServiceTest.java`

**Interfaces:**
- Consumes: `MatchingEngine`, `CandidateProfile`, `JobOpportunity`, `FilterConfiguration`
- Produces: Hexagonal Application Use Case and Ports Interfaces + Default Implementation Service.

- [ ] **Step 1: Write failing unit test for DefaultAssessJobCompatibilityService**

```java
package com.watashi.core.ports.in;

import junit.framework.TestCase;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.*;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.*;
import java.util.List;
import java.util.Set;

public class DefaultAssessJobCompatibilityServiceTest extends TestCase {

    public void testServiceEvaluation() {
        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase useCase = new DefaultAssessJobCompatibilityService(engine);

        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        CandidateProfile profile = new CandidateProfile("c1", "Dev", "Summary", Set.of(java), Set.of(SeniorityLevel.MID), Set.of(WorkMode.REMOTE), null, Set.of());
        JobOpportunity job = new JobOpportunity("j1", "Java Dev", "Company", "Desc", Set.of(java), Set.of(), SeniorityLevel.MID, WorkMode.REMOTE, "Remote", null, "url", null);

        MatchResult result = useCase.evaluate(job, profile, FilterConfiguration.defaultConfig());
        assertEquals(100.0, result.overallScore(), 0.01);
        assertEquals(MatchStatus.RECOMMENDED, result.status());

        List<MatchResult> listResult = useCase.evaluateAll(List.of(job), profile, FilterConfiguration.defaultConfig());
        assertEquals(1, listResult.size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=DefaultAssessJobCompatibilityServiceTest`
Expected: FAIL due to missing ports and service classes.

- [ ] **Step 3: Implement Inbound/Outbound Ports and Use Case Implementation**

`AssessJobCompatibilityUseCase.java`:
```java
package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import java.util.List;

public interface AssessJobCompatibilityUseCase {
    MatchResult evaluate(JobOpportunity job, CandidateProfile profile, FilterConfiguration config);
    List<MatchResult> evaluateAll(List<JobOpportunity> jobs, CandidateProfile profile, FilterConfiguration config);
}
```

`ManageCandidateProfileUseCase.java`:
```java
package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidateProfile;
import java.util.Optional;

public interface ManageCandidateProfileUseCase {
    Optional<CandidateProfile> getProfile();
    void updateProfile(CandidateProfile profile);
}
```

`ManageFilterConfigUseCase.java`:
```java
package com.watashi.core.ports.in;

import com.watashi.core.domain.matching.FilterConfiguration;

public interface ManageFilterConfigUseCase {
    FilterConfiguration getConfig();
    void updateConfig(FilterConfiguration config);
}
```

`CandidateProfileRepository.java`:
```java
package com.watashi.core.ports.out;

import com.watashi.core.domain.candidate.CandidateProfile;
import java.util.Optional;

public interface CandidateProfileRepository {
    Optional<CandidateProfile> findDefault();
    void save(CandidateProfile profile);
}
```

`JobRepository.java`:
```java
package com.watashi.core.ports.out;

import com.watashi.core.domain.job.JobOpportunity;
import java.util.List;
import java.util.Optional;

public interface JobRepository {
    void save(JobOpportunity job);
    List<JobOpportunity> findAll();
    Optional<JobOpportunity> findById(String id);
}
```

`FilterConfigRepository.java`:
```java
package com.watashi.core.ports.out;

import com.watashi.core.domain.matching.FilterConfiguration;

public interface FilterConfigRepository {
    FilterConfiguration load();
    void save(FilterConfiguration config);
}
```

`DefaultAssessJobCompatibilityService.java`:
```java
package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.domain.matching.MatchingEngine;
import java.util.List;
import java.util.Objects;

public class DefaultAssessJobCompatibilityService implements AssessJobCompatibilityUseCase {

    private final MatchingEngine engine;

    public DefaultAssessJobCompatibilityService(MatchingEngine engine) {
        this.engine = Objects.requireNonNull(engine, "MatchingEngine cannot be null");
    }

    @Override
    public MatchResult evaluate(JobOpportunity job, CandidateProfile profile, FilterConfiguration config) {
        return engine.evaluate(job, profile, config);
    }

    @Override
    public List<MatchResult> evaluateAll(List<JobOpportunity> jobs, CandidateProfile profile, FilterConfiguration config) {
        if (jobs == null) return List.of();
        return jobs.stream()
            .map(job -> evaluate(job, profile, config))
            .toList();
    }
}
```

- [ ] **Step 4: Run full test suite & spotless check**

Run: `mvn spotless:apply && mvn verify`
Expected: BUILD SUCCESS with all unit tests passing.

- [ ] **Step 5: Format and commit**

```bash
mvn spotless:apply
git add src/main/java/com/watashi/core/ports/ src/test/java/com/watashi/core/ports/
git commit -m "feat(core): add hexagonal ports and DefaultAssessJobCompatibilityService use case implementation"
```
