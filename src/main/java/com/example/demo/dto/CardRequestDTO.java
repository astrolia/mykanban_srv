package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record CardRequestDTO(
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
    public String resolvedLabel() {
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return null;
    }

    @AssertTrue(message = "Título do card não pode ficar em branco")
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

    public LocalDate resolvedDeadline() {
        String raw = deadline != null ? deadline : finalDate;
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed);
        }
        try {
            return Instant.parse(trimmed).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignored) {
            return OffsetDateTime.parse(trimmed).toLocalDate();
        }
    }
}
