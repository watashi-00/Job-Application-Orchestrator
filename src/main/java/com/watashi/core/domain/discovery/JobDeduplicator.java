package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.job.JobOpportunity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class JobDeduplicator {

    private static final Pattern LEGAL_SUFFIXES =
            Pattern.compile("\\b(inc|llc|ltd|corp|co|corporation|limited|incorporated)\\b", Pattern.CASE_INSENSITIVE);

    private JobDeduplicator() {
        // Utility class
    }

    public static List<JobOpportunity> deduplicate(List<JobOpportunity> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        Map<String, JobOpportunity> deduplicatedMap = new LinkedHashMap<>();

        for (JobOpportunity job : jobs) {
            if (job == null) {
                continue;
            }
            String key = normalizeCompany(job.company()) + "::" + normalizeTitle(job.title());
            JobOpportunity existing = deduplicatedMap.get(key);
            if (existing == null || calculateQualityScore(job) > calculateQualityScore(existing)) {
                deduplicatedMap.put(key, job);
            }
        }

        return new ArrayList<>(deduplicatedMap.values());
    }

    private static String normalizeCompany(String company) {
        if (company == null || company.isBlank()) {
            return "";
        }
        String normalized = company.toLowerCase();
        normalized = LEGAL_SUFFIXES.matcher(normalized).replaceAll("");
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        return title.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static int calculateQualityScore(JobOpportunity job) {
        int salaryScore = getSalaryRichness(job);
        int descLength = getDescriptionLength(job);
        return (salaryScore * 100) + descLength;
    }

    private static int getSalaryRichness(JobOpportunity job) {
        SalaryRange range = job.salaryRange();
        if (range == null) {
            return 0;
        }
        int score = 1;
        if (range.min() != null) {
            score++;
        }
        if (range.max() != null) {
            score++;
        }
        if (range.currency() != null && !range.currency().isBlank()) {
            score++;
        }
        return score;
    }

    private static int getDescriptionLength(JobOpportunity job) {
        return job.description() == null ? 0 : job.description().length();
    }
}
