package com.example.demo.dto;

import com.example.demo.model.enums.EnvironmentType;

public record EnvironmentRequestDTO(
        String name,
        String description,
        Integer position,
        Boolean archived,
        EnvironmentType type
) {
}
