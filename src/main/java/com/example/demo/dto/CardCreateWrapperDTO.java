package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CardCreateWrapperDTO(
        @Valid
        CardRequestDTO data,

        @Size(max = 150, message = "Título do card deve ter no máximo 150 caracteres")
        String label,

        @Size(max = 150, message = "Título do card deve ter no máximo 150 caracteres")
        String name,

        @Positive(message = "Coluna inválida")
        Integer column,

        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
        String description,

        @Positive(message = "Prioridade inválida")
        Integer priority,

        @DecimalMin(value = "0.0", message = "Progresso deve estar entre 0 e 100")
        @DecimalMax(value = "100.0", message = "Progresso deve estar entre 0 e 100")
        Float progress,

        String deadline,

        String finalDate,

        List<Integer> users,

        Boolean done,

        Boolean archived,

        @PositiveOrZero(message = "Posição inválida")
        Integer position
) {
    public CardRequestDTO unwrap() {
        if (data != null) {
            return data;
        }
        return new CardRequestDTO(label, name, column, description, priority, progress,
                deadline, finalDate, users, done, archived, position);
    }

    @AssertTrue(message = "Título do card é obrigatório")
    @JsonIgnore
    public boolean isLabelPresent() {
        CardRequestDTO resolved = unwrap();
        return resolved.resolvedLabel() != null;
    }
}
