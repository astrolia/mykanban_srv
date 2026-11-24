package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record PriorityRequestDTO(
        @Size(max = 100, message = "Nome da prioridade deve ter no máximo 100 caracteres")
        String label,

        @Size(max = 100, message = "Nome da prioridade deve ter no máximo 100 caracteres")
        String name,

        @Size(max = 50, message = "Cor inválida")
        String css,

        @Size(max = 50, message = "Cor inválida")
        String color
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

    public String resolvedCss() {
        if (css != null && !css.isBlank()) {
            return css.trim();
        }
        if (color != null && !color.isBlank()) {
            return color.trim();
        }
        return null;
    }

    @AssertTrue(message = "Nome da prioridade não pode ficar em branco")
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
