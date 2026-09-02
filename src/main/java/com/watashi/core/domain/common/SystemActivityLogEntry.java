package com.watashi.core.domain.common;

import java.util.Objects;

public record SystemActivityLogEntry(
        String id, String type, String title, String detail, String status, String timestamp, String gmailUrl) {

    public SystemActivityLogEntry {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(detail, "detail cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }
}
