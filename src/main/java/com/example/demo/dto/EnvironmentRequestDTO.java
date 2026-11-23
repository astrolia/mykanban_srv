package com.example.demo.dto;

import com.example.demo.model.enums.EnvironmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record EnvironmentRequestDTO(
        @NotBlank(message = "Nome do ambiente é obrigatório")
        @Size(max = 100, message = "Nome do ambiente deve ter no máximo 100 caracteres")
        String name,

        @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
        String description,

        @PositiveOrZero(message = "Posição inválida")
        Integer position,

        Boolean archived,

        EnvironmentType type
) {
}
