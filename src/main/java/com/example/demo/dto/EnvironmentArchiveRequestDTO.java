package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public record EnvironmentArchiveRequestDTO(
        @NotNull(message = "Status de arquivamento é obrigatório")
        Boolean archived
) {
}
