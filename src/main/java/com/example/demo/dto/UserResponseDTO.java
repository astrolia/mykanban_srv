package com.example.demo.dto;

import com.example.demo.model.UserModel;

public record UserResponseDTO(String name, String email) {

    public static UserResponseDTO from(UserModel userModel){
        return new UserResponseDTO(userModel.getName(), userModel.getEmail());
    }
}