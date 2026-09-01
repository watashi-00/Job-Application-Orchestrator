package com.watashi.core.domain.discovery;

public record JobQuery(String query, String category, int limit) {
    public JobQuery {
        query = query == null ? "" : query.trim();
        category = category == null ? "" : category.trim();
        if (limit <= 0) {
            limit = 50;
        }
    }

    public static JobQuery ofSoftwareDev() {
        return new JobQuery("", "software-dev", 50);
    }
}
