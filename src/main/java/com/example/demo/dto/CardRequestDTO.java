package com.example.demo.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record CardRequestDTO(
        String label,
        String name,
        Integer column,
        String description,
        Integer priority,
        Float progress,
        String deadline,
        String finalDate,
        List<Integer> users,
        Boolean done,
        Boolean archived,
        Integer position
) {
    public String resolvedLabel() {
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return null;
    }

    public LocalDate resolvedDeadline() {
        String raw = deadline != null ? deadline : finalDate;
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed);
        }
        try {
            return Instant.parse(trimmed).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignored) {
            return OffsetDateTime.parse(trimmed).toLocalDate();
        }
    }
}
