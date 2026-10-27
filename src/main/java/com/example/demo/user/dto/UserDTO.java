package com.example.demo.user.dto;

import com.example.demo.user.model.UserModel;

public record UserDTO(String name, String cpf, String email, String password) {

    public static UserDTO from(UserModel userModel){
        return new UserDTO(userModel.getName(), userModel.getCpf(), userModel.getEmail(), userModel.getPassword());
    }
}