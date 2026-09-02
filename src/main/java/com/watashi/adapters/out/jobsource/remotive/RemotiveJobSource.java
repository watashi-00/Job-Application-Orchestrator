package com.watashi.adapters.out.jobsource.remotive;

import com.watashi.core.domain.common.HtmlUtils;
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
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemotiveJobSource implements JobSource {

    private static final String SOURCE_NAME = "Remotive";
    private static final String DEFAULT_CATEGORY = "software-dev";
    private static final String BASE_URL = "https://remotive.com/api/remote-jobs?category=";

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(?:\"(.*?)\"|([^,\\}\\s]+))");
    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*(?:\"(.*?)\"|([^,\\}\\s]+))");
    private static final Pattern TITLE_PATTERN = Pattern.compile("\"title\"\\s*:\\s*(?:\"(.*?)\"|([^,\\}\\s]+))");
    private static final Pattern COMPANY_PATTERN =
            Pattern.compile("\"company_name\"\\s*:\\s*(?:\"(.*?)\"|([^,\\}\\s]+))");
    private static final Pattern LOCATION_PATTERN =
            Pattern.compile("\"candidate_required_location\"\\s*:\\s*(?:\"(.*?)\"|([^,\\}\\s]+))");
    private static final Pattern SALARY_PATTERN = Pattern.compile("\"salary\"\\s*:\\s*(?:\"(.*?)\"|([^,\\}\\s]+))");
    private static final Pattern TAGS_PATTERN = Pattern.compile("\"tags\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern TAG_ITEM_PATTERN = Pattern.compile("\"(.*?)\"");

    private static final Pattern SALARY_PARSE_PATTERN = Pattern.compile(
            "\\$?\\s*([0-9]{1,3}(?:,[0-9]{3})*|\\d+)\\s*(?:-\\s*\\$?\\s*([0-9]{1,3}(?:,[0-9]{3})*|\\d+))?");

    private final HttpEngine httpEngine;

    public RemotiveJobSource(HttpEngine httpEngine) {
        this.httpEngine = Objects.requireNonNullElseGet(httpEngine, HttpEngine::createDefault);
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<JobOpportunity> fetchJobs(JobQuery query) {
        String category = DEFAULT_CATEGORY;
        if (query != null && query.category() != null && !query.category().isBlank()) {
            category = query.category();
        }

        String url = BASE_URL + URLEncoder.encode(category, StandardCharsets.UTF_8);
        try {
            HttpRequestSpec spec = new HttpRequestSpec(URI.create(url), Map.of());
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
        List<String> objectStrings = extractJsonObjectStrings(json);
        List<JobOpportunity> opportunities = new ArrayList<>();

        for (String objJson : objectStrings) {
            String rawId = extractField(ID_PATTERN, objJson);
            if (rawId.isBlank()) {
                continue;
            }

            String jobId = rawId.startsWith("remotive-") ? rawId : "remotive-" + rawId;
            String title = extractField(TITLE_PATTERN, objJson);
            String company = extractField(COMPANY_PATTERN, objJson);
            String description = extractDescription(objJson);
            String location = extractField(LOCATION_PATTERN, objJson);
            String salaryStr = extractField(SALARY_PATTERN, objJson);
            String url = extractField(URL_PATTERN, objJson);
            String tagsStr = extractTagsString(objJson);

            String combinedTextForSkills = (title + " " + tagsStr + " " + description).trim();
            Set<Skill> requiredSkills = SkillExtractor.extractSkills(combinedTextForSkills);
            SeniorityLevel seniorityLevel = SkillExtractor.inferSeniority(title);
            SalaryRange salaryRange = parseSalaryRange(salaryStr);

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
    }

    private static List<String> extractJsonObjectStrings(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        int jobsIdx = json.indexOf("\"jobs\"");
        if (jobsIdx == -1) {
            return List.of();
        }
        int startArray = json.indexOf('[', jobsIdx);
        if (startArray == -1) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        boolean inString = false;
        boolean escape = false;
        int depth = 0;
        StringBuilder current = null;

        for (int i = startArray + 1; i < json.length(); i++) {
            char c = json.charAt(i);

            if (inString) {
                if (current != null) {
                    current.append(c);
                }
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                    if (current != null) {
                        current.append(c);
                    }
                } else if (c == '{') {
                    if (depth == 0) {
                        current = new StringBuilder();
                    }
                    depth++;
                    current.append(c);
                } else if (c == '}') {
                    depth--;
                    if (current != null) {
                        current.append(c);
                    }
                    if (depth == 0 && current != null) {
                        results.add(current.toString());
                        current = null;
                    }
                } else if (c == ']' && depth == 0) {
                    break;
                } else {
                    if (current != null) {
                        current.append(c);
                    }
                }
            }
        }
        return results;
    }

    private static String extractDescription(String objJson) {
        int keyIdx = objJson.indexOf("\"description\"");
        if (keyIdx == -1) {
            return "";
        }
        int colonIdx = objJson.indexOf(':', keyIdx);
        if (colonIdx == -1) {
            return "";
        }
        int quoteStart = objJson.indexOf('"', colonIdx + 1);
        if (quoteStart == -1) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = quoteStart + 1; i < objJson.length(); i++) {
            char c = objJson.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        String rawHtml = unescapeJson(sb.toString());
        return HtmlUtils.stripHtml(rawHtml);
    }

    private static String extractField(Pattern pattern, String objJson) {
        Matcher matcher = pattern.matcher(objJson);
        if (matcher.find()) {
            String val = matcher.group(1);
            if (val == null) {
                val = matcher.group(2);
            }
            if (val != null) {
                val = val.trim();
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1);
                }
                if ("null".equalsIgnoreCase(val)) {
                    return "";
                }
                return unescapeJson(val);
            }
        }
        return "";
    }

    private static String extractTagsString(String objJson) {
        Matcher matcher = TAGS_PATTERN.matcher(objJson);
        if (!matcher.find()) {
            return "";
        }
        String arrayContent = matcher.group(1);
        Matcher itemMatcher = TAG_ITEM_PATTERN.matcher(arrayContent);

        List<String> tags = new ArrayList<>();
        while (itemMatcher.find()) {
            tags.add(unescapeJson(itemMatcher.group(1)));
        }
        return String.join(" ", tags);
    }

    private static SalaryRange parseSalaryRange(String salaryStr) {
        if (salaryStr == null || salaryStr.isBlank()) {
            return null;
        }

        Matcher matcher = SALARY_PARSE_PATTERN.matcher(salaryStr);
        if (!matcher.find()) {
            return null;
        }

        try {
            String minRaw = matcher.group(1).replace(",", "");
            BigDecimal min = new BigDecimal(minRaw);

            BigDecimal max = null;
            if (matcher.group(2) != null) {
                String maxRaw = matcher.group(2).replace(",", "");
                max = new BigDecimal(maxRaw);
            }

            return new SalaryRange(min, max, "USD");
        } catch (Exception e) {
            return null;
        }
    }

    private static String unescapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\u00a0", " ")
                .replace("\\u2192", "->");
    }
}
