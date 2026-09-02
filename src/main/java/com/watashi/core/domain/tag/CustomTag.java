package com.watashi.core.domain.tag;

import java.util.Objects;

public record CustomTag(String id, String name, String category, String colorHex, boolean isUserCreated) {

    public CustomTag {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        name = name.trim();
        if (id == null || id.isBlank()) {
            id = name.toLowerCase().replaceAll("[^a-z0-9]", "-");
        }
        if (category == null || category.isBlank()) {
            category = "GENERAL";
        } else {
            category = category.trim();
        }
        if (colorHex == null || colorHex.isBlank()) {
            colorHex = "#6c757d";
        } else {
            colorHex = colorHex.trim();
        }
    }
}
