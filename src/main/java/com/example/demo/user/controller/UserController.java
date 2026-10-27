package com.example.demo.user.controller;

import com.example.demo.user.dto.UserDTO;
import com.example.demo.user.model.UserModel;
import com.example.demo.user.service.UserService;
import jakarta.validation.Valid; // Se estiver usando Spring Boot 3+ (ou javax.validation se for versão antiga)
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alunos")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDTO> create(@RequestBody @Valid UserDTO userDTO) {
        UserModel savedUser = userService.save(new UserModel(
                userDTO.name(),
                userDTO.email(),
                userDTO.cpf(),
                userDTO.password()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(UserDTO.from(savedUser));
    }
}