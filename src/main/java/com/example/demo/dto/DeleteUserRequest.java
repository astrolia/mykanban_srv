package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteUserRequest(
        @NotBlank(message = "Senha é obrigatória")
        String password
) {
}
