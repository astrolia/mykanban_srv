package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.model.UserModel;
import com.example.demo.service.TokenService;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login e geração de token JWT")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Operation(summary = "Autentica usuário e retorna token JWT")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req) {
        UserModel user = userService.findByEmail(req.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            log.warn("Tentativa de login inválida para email={}", req.email());
            return ResponseEntity.status(401).body(Map.of("message", "Email ou senha inválidos"));
        }

        log.info("Login realizado para usuário id={}", user.getId());
        return ResponseEntity.ok(Map.of("token", tokenService.generate(user.getEmail())));
    }
}
