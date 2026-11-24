package com.example.demo.dto;

public record ColumnRequestDTO(String label, String name, Integer position) {

    public String resolvedLabel() {
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return null;
    }
}
