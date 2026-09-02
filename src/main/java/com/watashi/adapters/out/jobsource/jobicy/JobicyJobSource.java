package com.watashi.adapters.out.jobsource.jobicy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.watashi.adapters.out.jobsource.remotive.RemotiveJobSource;
import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.discovery.SkillExtractor;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.out.JobSource;
import com.watashi.infrastructure.http.HttpEngine;
import com.watashi.infrastructure.http.HttpRequestSpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class JobicyJobSource implements JobSource {

    private static final String SOURCE_NAME = "Jobicy";
    private static final String DEFAULT_URL = "https://jobicy.com/api/v2/remote-jobs";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpEngine httpEngine;

    public JobicyJobSource() {
        this(null);
    }

    public JobicyJobSource(HttpEngine httpEngine) {
        this.httpEngine = Objects.requireNonNullElseGet(httpEngine, HttpEngine::createDefault);
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<JobOpportunity> fetchJobs(JobQuery query) {
        try {
            HttpRequestSpec spec = new HttpRequestSpec(URI.create(DEFAULT_URL), Map.of());
            CompletableFuture<HttpResponse<String>> future = httpEngine.fetch(spec);
            HttpResponse<String> response = future.join();
            if (response != null && response.statusCode() == 200 && response.body() != null) {
                List<JobOpportunity> jobs = parseJobsResponse(response.body());
                if (query != null && query.limit() > 0 && jobs.size() > query.limit()) {
                    return jobs.subList(0, query.limit());
                }
                return jobs;
            }
        } catch (Exception e) {
            // Return empty list on failure
        }
        return List.of();
    }

    public static List<JobOpportunity> parseJobsResponse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root == null || !root.has("jobs") || !root.get("jobs").isArray()) {
                return List.of();
            }

            JsonNode jobsNode = root.get("jobs");
            List<JobOpportunity> opportunities = new ArrayList<>();

            for (JsonNode item : jobsNode) {
                String rawId = getFieldAsString(item, "id");
                if (rawId.isBlank()) {
                    rawId = getFieldAsString(item, "slug");
                }
                if (rawId.isBlank()) {
                    continue;
                }

                String jobId = rawId.startsWith("jobicy-") ? rawId : "jobicy-" + rawId;
                String title = getFirstFieldAsString(item, "jobTitle", "title");
                String company = getFirstFieldAsString(item, "companyName", "company");
                String rawDescription = getFirstFieldAsString(item, "jobDescription", "description");
                String description = RemotiveJobSource.stripHtml(rawDescription);
                String location = getFirstFieldAsString(item, "jobGeo", "location");
                String url = getFieldAsString(item, "url");

                SalaryRange salaryRange = parseSalaryRange(item);

                String textForSkills = (title + " " + description).trim();
                Set<Skill> requiredSkills = SkillExtractor.extractSkills(textForSkills);
                SeniorityLevel seniorityLevel = SkillExtractor.inferSeniority(title);

                JobOpportunity opportunity = new JobOpportunity(
                        jobId,
                        title,
                        company,
                        description,
                        requiredSkills,
                        Set.of(),
                        seniorityLevel,
                        WorkMode.REMOTE,
                        location,
                        salaryRange,
                        url,
                        JobStatus.DISCOVERED);

                opportunities.add(opportunity);
            }

            return List.copyOf(opportunities);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static SalaryRange parseSalaryRange(JsonNode item) {
        String minStr = getFieldAsString(item, "annualSalaryMin");
        String maxStr = getFieldAsString(item, "annualSalaryMax");

        BigDecimal min = parseBigDecimal(minStr);
        BigDecimal max = parseBigDecimal(maxStr);

        if (min == null && max == null) {
            return null;
        }

        String currency = getFieldAsString(item, "salaryCurrency");
        if (currency.isBlank()) {
            currency = "USD";
        }

        return new SalaryRange(min, max, currency);
    }

    private static BigDecimal parseBigDecimal(String valueStr) {
        if (valueStr == null || valueStr.isBlank()) {
            return null;
        }
        try {
            String clean = valueStr.replaceAll("[^0-9.]", "");
            if (clean.isBlank()) {
                return null;
            }
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getFieldAsString(JsonNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            return node.get(fieldName).asText("").trim();
        }
        return "";
    }

    private static String getFirstFieldAsString(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String val = getFieldAsString(node, fieldName);
            if (!val.isBlank()) {
                return val;
            }
        }
        return "";
    }
}
