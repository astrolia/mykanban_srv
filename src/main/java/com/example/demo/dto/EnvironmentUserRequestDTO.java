package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EnvironmentUserRequestDTO(
        @NotNull(message = "userId é obrigatório")
        @Positive(message = "userId inválido")
        Integer userId
) {
}
