package com.example.demo.dto;

import com.example.demo.model.enums.EnvironmentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record EnvironmentUpdateRequestDTO(
        @Size(max = 100, message = "Nome do ambiente deve ter no máximo 100 caracteres")
        String name,

        @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
        String description,

        @PositiveOrZero(message = "Posição inválida")
        Integer position,

        Boolean archived,

        EnvironmentType type
) {
    @AssertTrue(message = "Nome do ambiente é obrigatório")
    @JsonIgnore
    public boolean isNameValid() {
        return name == null || !name.isBlank();
    }
}
