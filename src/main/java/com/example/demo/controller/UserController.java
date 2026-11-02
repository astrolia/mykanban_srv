package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.model.UserModel;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@RequestBody @Valid UserDTO userDTO) {
        UserModel savedUser = userService.saveUser(new UserModel(
                userDTO.name(),
                userDTO.email(),
                userDTO.password()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.from(savedUser));
    }
}