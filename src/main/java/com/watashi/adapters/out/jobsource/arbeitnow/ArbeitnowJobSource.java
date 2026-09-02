package com.watashi.adapters.out.jobsource.arbeitnow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.watashi.adapters.out.jobsource.remotive.RemotiveJobSource;
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
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ArbeitnowJobSource implements JobSource {

    private static final String SOURCE_NAME = "Arbeitnow";
    private static final String DEFAULT_URL = "https://www.arbeitnow.com/api/job-board-api";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpEngine httpEngine;

    public ArbeitnowJobSource() {
        this(null);
    }

    public ArbeitnowJobSource(HttpEngine httpEngine) {
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
            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                return List.of();
            }

            JsonNode dataNode = root.get("data");
            List<JobOpportunity> opportunities = new ArrayList<>();

            for (JsonNode item : dataNode) {
                String rawSlug = getFieldAsString(item, "slug");
                if (rawSlug.isBlank()) {
                    continue;
                }

                String jobId = rawSlug.startsWith("arbeitnow-") ? rawSlug : "arbeitnow-" + rawSlug;
                String company = getFieldAsString(item, "company_name");
                String title = getFieldAsString(item, "title");
                String rawDescription = getFieldAsString(item, "description");
                String description = RemotiveJobSource.stripHtml(rawDescription);
                String location = getFieldAsString(item, "location");
                String url = getFieldAsString(item, "url");

                List<String> tags = new ArrayList<>();
                if (item.has("tags") && item.get("tags").isArray()) {
                    for (JsonNode tagNode : item.get("tags")) {
                        if (tagNode.isTextual()) {
                            tags.add(tagNode.asText());
                        }
                    }
                }
                String tagsStr = String.join(" ", tags);

                String textForSkills = (title + " " + tagsStr + " " + description).trim();
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
                        null,
                        url,
                        JobStatus.DISCOVERED);

                opportunities.add(opportunity);
            }

            return List.copyOf(opportunities);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String getFieldAsString(JsonNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            return node.get(fieldName).asText("").trim();
        }
        return "";
    }
}
