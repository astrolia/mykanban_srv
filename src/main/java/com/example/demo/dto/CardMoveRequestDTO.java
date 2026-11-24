package com.example.demo.dto;

import jakarta.validation.constraints.Positive;

public record CardMoveRequestDTO(
        @Positive(message = "Coluna inválida")
        Integer column,

        @Positive(message = "Card de referência inválido")
        Integer before
) {
}
