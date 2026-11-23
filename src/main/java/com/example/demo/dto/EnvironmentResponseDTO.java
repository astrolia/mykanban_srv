package com.example.demo.dto;

import com.example.demo.model.EnvironmentModel;

import java.util.List;

public record EnvironmentResponseDTO(
        Integer id,
        String name,
        String description,
        Integer owner,
        Integer position,
        Boolean archived,
        List<PriorityResponseDTO> priority,
        List<ColumnResponseDTO> columns,
        List<CardResponseDTO> cards,
        List<UserEnvDTO> users
) {
    public static EnvironmentResponseDTO from(EnvironmentModel env) {
        return new EnvironmentResponseDTO(
                env.getId(),
                env.getName(),
                env.getDescription() != null ? env.getDescription() : "",
                env.getOwner().getId(),
                env.getPosition(),
                env.getArchived(),
                env.getPriorities().stream().map(PriorityResponseDTO::from).toList(),
                env.getColumns().stream().map(ColumnResponseDTO::from).toList(),
                env.getCards().stream().map(CardResponseDTO::from).toList(),
                env.getUsers().stream().map(UserEnvDTO::from).toList()
        );
    }
}
