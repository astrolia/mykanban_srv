package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record PriorityCreateWrapperDTO(
        @Valid
        PriorityRequestDTO data,

        @Size(max = 100, message = "Nome da prioridade deve ter no máximo 100 caracteres")
        String label,

        @Size(max = 100, message = "Nome da prioridade deve ter no máximo 100 caracteres")
        String name,

        @Size(max = 50, message = "Cor inválida")
        String css,

        @Size(max = 50, message = "Cor inválida")
        String color
) {
    public PriorityRequestDTO unwrap() {
        if (data != null) {
            return data;
        }
        return new PriorityRequestDTO(label, name, css, color);
    }

    @AssertTrue(message = "Nome da prioridade é obrigatório")
    @JsonIgnore
    public boolean isLabelPresent() {
        PriorityRequestDTO resolved = unwrap();
        return resolved.resolvedLabel() != null;
    }
}
