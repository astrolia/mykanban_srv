package com.example.demo.dto;

public record PriorityRequestDTO(String label, String name, String css, String color) {

    public String resolvedLabel() {
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return null;
    }

    public String resolvedCss() {
        if (css != null && !css.isBlank()) {
            return css.trim();
        }
        if (color != null && !color.isBlank()) {
            return color.trim();
        }
        return null;
    }
}
