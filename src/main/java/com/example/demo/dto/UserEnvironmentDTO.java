package com.example.demo.dto;

import com.example.demo.model.EnvironmentModel;

public record UserEnvironmentDTO(
        Integer id,
        String name,
        String description,
        Integer owner,
        Boolean archived
) {
    public static UserEnvironmentDTO from(EnvironmentModel env) {
        return new UserEnvironmentDTO(
                env.getId(),
                env.getName(),
                env.getDescription() != null ? env.getDescription() : "",
                env.getOwner().getId(),
                env.getArchived()
        );
    }
}
