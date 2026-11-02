package com.example.demo.dto;

import com.example.demo.model.UserModel;

public record UserDTO(String name, String email, String password) {

    public static UserDTO from(UserModel userModel){
        return new UserDTO(userModel.getName(), userModel.getEmail(), userModel.getPassword());
    }
}