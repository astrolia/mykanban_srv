package com.example.demo.dto;

import com.example.demo.model.UserModel;

public record UserResponseDTO(Integer id, String name, String email) {

    public static UserResponseDTO from(UserModel userModel) {
        return new UserResponseDTO(userModel.getId(), userModel.getName(), userModel.getEmail());
    }
}
