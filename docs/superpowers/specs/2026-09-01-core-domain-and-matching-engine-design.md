# Technical Specification: Core Domain & Matching Engine (MVP 0.0.1)

**Date**: 2026-09-01  
**Project**: Job Application Orchestrator  
**Module**: Core (Domain Model, Candidate Profile, Filter Configuration & Matching Engine)

---

## 1. Overview

The **Core Domain & Matching Engine** forms the central intelligence of the Job Application Orchestrator. It models the Candidate Profile, Job Opportunities, Filter Configurations, and calculates objective compatibility scores (0-100%) with explicit breakdowns, highlighted skill gaps, missing requirements, and potential conflicts.

The implementation follows **Clean Architecture / Hexagonal Architecture** principles using modern **Java 21** features including `record` types and `sealed` interfaces/enums for immutable domain models.

---

## 2. Architecture & Package Structure

The project code resides under `com.watashi`, organized as follows:

```
com.watashi
├── core
│   ├── domain
│   │   ├── candidate       # Candidate Profile, Skills, Preferences
│   │   ├── job             # Job Opportunity, Requirements, Salary
│   │   ├── matching        # Score, MatchResult, ScoreBreakdown, MatchingEngine
│   │   └── common          # Value Objects (SeniorityLevel, WorkMode, Money, Location)
│   └── ports
│       ├── in              # Inbound Ports / Use Cases (AssessJobCompatibilityUseCase, etc.)
│       └── out             # Outbound Ports / Repositories (CandidateProfileRepository, etc.)
├── adapters
│   ├── in                  # CLI and HTTP Dashboard controllers
│   └── out                 # PDF Ingestor, Job Sources, Local Persistence
└── infrastructure
    ├── concurrency         # ThreadManager
    └── http                # HttpEngine
```

---

## 3. Domain Model Specifications

### 3.1 Common Value Objects (`com.watashi.core.domain.common`)

- **`SeniorityLevel` (Enum)**: `INTERN`, `JUNIOR`, `MID`, `SENIOR`, `LEAD`, `PRINCIPAL`.
- **`WorkMode` (Enum)**: `REMOTE`, `HYBRID`, `ONSITE`.
- **`SkillCategory` (Enum)**: `LANGUAGES_FRAMEWORKS`, `DATABASE`, `DEVOPS_CLOUD`, `ARCHITECTURE_DESIGN`, `METHODOLOGY_SOFT_SKILLS`, `OTHER`.
- **`Skill` (Record)**:
  - `String name`: Skill name (normalized lowercase comparison).
  - `SkillCategory category`: Classification.
  - `int yearsExperience`: Years of experience (0 if unstated).
- **`SalaryRange` (Record)**:
  - `BigDecimal min`: Minimum salary.
  - `BigDecimal max`: Maximum salary.
  - `String currency`: Currency code (e.g. "BRL", "USD").

### 3.2 Candidate Profile (`com.watashi.core.domain.candidate`)

- **`CandidateProfile` (Record)**:
  - `String id`: Unique identifier.
  - `String title`: Primary target job title (e.g., "Senior Java Backend Engineer").
  - `String summary`: Bio or summary overview.
  - `Set<Skill> skills`: Set of technical and professional skills.
  - `Set<SeniorityLevel> targetSeniorities`: Accepted seniority levels.
  - `Set<WorkMode> preferredWorkModes`: Accepted work modes.
  - `SalaryRange desiredSalary`: Target salary range.
  - `Set<String> preferredLocations`: Target cities/regions or "REMOTE".

### 3.3 Job Opportunity (`com.watashi.core.domain.job`)

- **`JobOpportunity` (Record)**:
  - `String id`: Unique identifier.
  - `String title`: Job position title.
  - `String company`: Hiring company name.
  - `String description`: Full raw job description.
  - `Set<Skill> requiredSkills`: Mandatory required skills.
  - `Set<Skill> optionalSkills`: Nice-to-have / optional skills.
  - `SeniorityLevel seniorityLevel`: Target seniority requirement.
  - `WorkMode workMode`: Work location mode (REMOTE, HYBRID, ONSITE).
  - `String location`: Physical location or region.
  - `SalaryRange salaryRange`: Disclosed salary range (may be null/empty if undisclosed).
  - `String sourceUrl`: Source link / ATS listing URL.
  - `JobStatus status`: `DISCOVERED`, `EVALUATED`, `APPLIED`, `INTERVIEWING`, `REJECTED`, `OFFER`.

### 3.4 Filter Configuration (`com.watashi.core.domain.matching`)

- **`FilterConfiguration` (Record)**:
  - `double techWeight`: Weight for technical skills match (default `0.50`).
  - `double seniorityWeight`: Weight for seniority match (default `0.20`).
  - `double workModeWeight`: Weight for work mode match (default `0.15`).
  - `double salaryWeight`: Weight for salary match (default `0.15`).
  - `double minimumScoreThreshold`: Minimum overall score for `RECOMMENDED` status (default `75.0`).
  - `boolean strictRequiredSkills`: If `true`, missing any required skill immediately sets status to `REJECTED` (default `false`).

### 3.5 Match Result (`com.watashi.core.domain.matching`)

- **`MatchStatus` (Enum)**: `RECOMMENDED`, `CONDITIONAL`, `REJECTED`.
- **`ScoreBreakdown` (Record)**:
  - `double techScore`: 0-100 score for skills match.
  - `double seniorityScore`: 0-100 score for seniority match.
  - `double workModeScore`: 0-100 score for work mode match.
  - `double salaryScore`: 0-100 score for salary match.
- **`MatchResult` (Record)**:
  - `String jobId`: ID of evaluated job.
  - `double overallScore`: Weighted total score (0.0 to 100.0).
  - `ScoreBreakdown breakdown`: Individual component scores.
  - `Set<Skill> matchedSkills`: Skills shared by candidate and job.
  - `Set<Skill> missingRequiredSkills`: Mandatory job skills missing from candidate profile.
  - `Set<Skill> missingOptionalSkills`: Optional job skills missing from candidate profile.
  - `List<String> conflicts`: List of explicit incompatibilities (e.g., location/work mode mismatch).
  - `MatchStatus status`: Final evaluation verdict.

---

## 4. Matching Engine Logic

The `MatchingEngine` class is a pure functional domain service without side effects.

### Algorithm Steps

1. **Technical Skills Assessment (`techScore`)**:
   - `requiredMatchRatio = (matchedRequiredSkills.size() / max(1, totalRequiredSkills))`
   - `optionalMatchRatio = (matchedOptionalSkills.size() / max(1, totalOptionalSkills))`
   - `techScore = (requiredMatchRatio * 85.0) + (optionalMatchRatio * 15.0)`
   - If `strictRequiredSkills == true` and `missingRequiredSkills` is non-empty:
     - Mark status as `REJECTED`.
     - Add conflict: *"Missing mandatory required skill(s): [names]"*.

2. **Seniority Assessment (`seniorityScore`)**:
   - If candidate `targetSeniorities` contains job `seniorityLevel`: `seniorityScore = 100.0`.
   - If job seniority is 1 step adjacent: `seniorityScore = 60.0`.
   - Otherwise: `seniorityScore = 0.0` and record a conflict.

3. **Work Mode Assessment (`workModeScore`)**:
   - If candidate `preferredWorkModes` contains job `workMode`: `workModeScore = 100.0`.
   - If candidate is strictly `REMOTE` and job is `ONSITE`: `workModeScore = 0.0` and add conflict *"Job requires ONSITE work but candidate only accepts REMOTE"*.
   - Otherwise: `workModeScore = 50.0`.

4. **Salary Assessment (`salaryScore`)**:
   - If job `salaryRange` is missing/undisclosed: `salaryScore = 100.0` (neutral, no penalty).
   - If job `salary.max` >= candidate `desiredSalary.min`: `salaryScore = 100.0`.
   - If job `salary.max` < candidate `desiredSalary.min`: Calculate ratio `(job.max / candidate.min) * 100.0` and record conflict *"Offered salary below candidate minimum expectation"*.

5. **Overall Weighted Score & Verdict**:
   - `overallScore = (techScore * techWeight) + (seniorityScore * seniorityWeight) + (workModeScore * workModeWeight) + (salaryScore * salaryWeight)`
   - If `overallScore >= minimumScoreThreshold` and no blocking conflicts: `status = RECOMMENDED`.
   - Else if `overallScore >= 50.0`: `status = CONDITIONAL`.
   - Else: `status = REJECTED`.

---

## 5. Use Cases & Ports

### 5.1 Inbound Ports (`core/ports/in`)

- **`AssessJobCompatibilityUseCase`**:
  ```java
  public interface AssessJobCompatibilityUseCase {
      MatchResult evaluate(JobOpportunity job, CandidateProfile profile, FilterConfiguration config);
      List<MatchResult> evaluateAll(List<JobOpportunity> jobs, CandidateProfile profile, FilterConfiguration config);
  }
  ```

- **`ManageCandidateProfileUseCase`**:
  ```java
  public interface ManageCandidateProfileUseCase {
      CandidateProfile getProfile();
      void updateProfile(CandidateProfile profile);
  }
  ```

- **`ManageFilterConfigUseCase`**:
  ```java
  public interface ManageFilterConfigUseCase {
      FilterConfiguration getConfig();
      void updateConfig(FilterConfiguration config);
  }
  ```

### 5.2 Outbound Ports (`core/ports/out`)

- **`CandidateProfileRepository`**:
  ```java
  public interface CandidateProfileRepository {
      Optional<CandidateProfile> findDefault();
      void save(CandidateProfile profile);
  }
  ```

- **`JobRepository`**:
  ```java
  public interface JobRepository {
      void save(JobOpportunity job);
      List<JobOpportunity> findAll();
      Optional<JobOpportunity> findById(String id);
  }
  ```

- **`FilterConfigRepository`**:
  ```java
  public interface FilterConfigRepository {
      FilterConfiguration load();
      void save(FilterConfiguration config);
  }
  ```

---

## 6. Verification Plan

1. **Unit Testing**:
   - Comprehensive JUnit tests for `MatchingEngineTest` covering:
     - Perfect match (100% score).
     - Missing optional skills vs missing required skills.
     - Strict required skill enforcement (`strictRequiredSkills=true`).
     - Seniority mismatch penalty.
     - Work mode conflict (Remote vs Onsite).
     - Undisclosed salary vs salary below minimum expectation.
     - Threshold boundary tests for `RECOMMENDED`, `CONDITIONAL`, `REJECTED`.
2. **Code Style & Formatting**:
   - Enforce Palantir format with `mvn spotless:check`.
3. **Build & Test Suite**:
   - Run `mvn verify` to confirm all tests pass cleanly.
