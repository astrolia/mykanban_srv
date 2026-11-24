package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.DeleteUserRequest;
import com.example.demo.dto.EnvironmentResponseDTO;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserEnvDTO;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.model.UserModel;
import com.example.demo.service.EnvironmentService;
import com.example.demo.service.TokenService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/users", "/user"})
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EnvironmentService environmentService;

    public UserController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          EnvironmentService environmentService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.environmentService = environmentService;
    }

    // POST /users -> cadastro (público)
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody @Valid UserDTO userDTO) {
        UserModel savedUser = userService.saveUser(new UserModel(
                userDTO.name(),
                userDTO.email(),
                userDTO.password()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.from(savedUser));
    }

    // GET /users/me -> dados do usuário logado
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResponseDTO.from(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado")));
    }

    // PUT /users/me -> atualiza nome e email
    @PutMapping("/me")
    public ResponseEntity<?> update(Authentication auth, @RequestBody UpdateUserRequest req) {
        UserModel user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado"));
        }

        if (req == null || req.name() == null || req.name().isBlank()
                || req.email() == null || req.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nome e email são obrigatórios"));
        }

        String normalizedEmail = req.email().trim().toLowerCase();
        var existing = userService.findByEmail(normalizedEmail);
        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            return ResponseEntity.status(409).body(Map.of("message", "Este email já está em uso"));
        }

        user.setName(req.name().trim());
        user.setEmail(normalizedEmail);
        userService.update(user);

        return ResponseEntity.ok(Map.of(
                "user", UserResponseDTO.from(user),
                "token", tokenService.generate(user.getEmail())
        ));
    }

    // PUT /users/me/password -> troca a senha
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody ChangePasswordRequest req) {
        UserModel user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado"));
        }

        if (req == null || req.currentPassword() == null
                || !passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("message", "Senha atual incorreta"));
        }

        if (req.newPassword() == null || req.newPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "A nova senha deve ter pelo menos 6 caracteres"));
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userService.update(user);

        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }

    // DELETE /users/me -> exclui a conta
    @DeleteMapping("/me")
    public ResponseEntity<?> delete(Authentication auth, @RequestBody DeleteUserRequest req) {
        UserModel user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado"));
        }

        if (req == null || req.password() == null
                || !passwordEncoder.matches(req.password(), user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("message", "Senha incorreta"));
        }

        userService.deleteById(user.getId());
        return ResponseEntity.noContent().build();
    }

    // GET /users/by-email?email=... -> busca usuário para adicionar em ambiente (retorna userEnv: { id, label })
    @GetMapping("/by-email")
    public ResponseEntity<?> findByEmail(@RequestParam String email) {
        return userService.findByEmail(email)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserEnvDTO.from(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado")));
    }

    // GET /users/{userId}/environments -> lista os ambientes do usuário
    @GetMapping({"/{userId}/environments", "/{userId}/environment"})
    public ResponseEntity<List<EnvironmentResponseDTO>> listUserEnvironments(
            @PathVariable Integer userId,
            Authentication auth) {
        return ResponseEntity.ok(environmentService.listForUser(auth.getName()));
    }

    // GET /users?email=... -> compatibilidade
    @GetMapping
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        return userService.findByEmail(email)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResponseDTO.from(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado")));
    }
}
