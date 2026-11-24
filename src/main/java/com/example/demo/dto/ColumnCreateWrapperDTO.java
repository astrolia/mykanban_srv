package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ColumnCreateWrapperDTO(
        @Valid
        ColumnRequestDTO column,

        @Size(max = 100, message = "Nome da coluna deve ter no máximo 100 caracteres")
        String label,

        @Size(max = 100, message = "Nome da coluna deve ter no máximo 100 caracteres")
        String name,

        @PositiveOrZero(message = "Posição inválida")
        Integer position
) {
    public ColumnRequestDTO unwrap() {
        if (column != null) {
            return column;
        }
        return new ColumnRequestDTO(label, name, position);
    }

    @AssertTrue(message = "Nome da coluna é obrigatório")
    @JsonIgnore
    public boolean isLabelPresent() {
        ColumnRequestDTO resolved = unwrap();
        return resolved.resolvedLabel() != null;
    }
}
