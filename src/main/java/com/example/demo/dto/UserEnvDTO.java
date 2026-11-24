package com.example.demo.dto;

import com.example.demo.model.UserModel;

public record UserEnvDTO(Integer id, String label) {

    public static UserEnvDTO from(UserModel user) {
        return new UserEnvDTO(user.getId(), user.getName());
    }
}
