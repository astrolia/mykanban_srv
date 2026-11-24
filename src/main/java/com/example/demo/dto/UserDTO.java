package com.example.demo.dto;

import com.example.demo.model.UserModel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDTO(
        @NotBlank(message = "Nome é obrigatório") String name,
        @NotBlank(message = "Email é obrigatório") @Email(message = "Email inválido") String email,
        @NotBlank(message = "Senha é obrigatória") String password
) {
    public static UserDTO from(UserModel userModel) {
        return new UserDTO(userModel.getName(), userModel.getEmail(), userModel.getPassword());
    }
}
