package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.model.UserModel;
import com.example.demo.service.TokenService;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null || req.email() == null || req.password() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Email ou senha inválidos"));
        }

        UserModel user = userService.findByEmail(req.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Email ou senha inválidos"));
        }

        return ResponseEntity.ok(Map.of("token", tokenService.generate(user.getEmail())));
    }
}
