package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ColumnRequestDTO(
        @Size(max = 100, message = "Nome da coluna deve ter no máximo 100 caracteres")
        String label,

        @Size(max = 100, message = "Nome da coluna deve ter no máximo 100 caracteres")
        String name,

        @PositiveOrZero(message = "Posição inválida")
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

    @AssertTrue(message = "Nome da coluna não pode ficar em branco")
    @JsonIgnore
    public boolean isLabelValid() {
        if (label != null && label.isBlank()) {
            return false;
        }
        if (name != null && name.isBlank()) {
            return false;
        }
        return true;
    }
}
