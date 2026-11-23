package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.DeleteUserRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserEnvDTO;
import com.example.demo.dto.UserEnvironmentDTO;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.model.UserModel;
import com.example.demo.service.EnvironmentService;
import com.example.demo.service.TokenService;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Cadastro, perfil e consulta de usuários")
public class UserController {

    private final UserService userService;
    private final EnvironmentService environmentService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Operation(summary = "Cadastra um novo usuário")
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody @Valid UserDTO userDTO) {
        UserModel savedUser = userService.saveUser(new UserModel(
                userDTO.name(),
                userDTO.email(),
                userDTO.password()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.from(savedUser));
    }

    @Operation(summary = "Retorna os dados do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResponseDTO.from(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado")));
    }

    @Operation(summary = "Atualiza nome e email do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/me")
    public ResponseEntity<?> update(Authentication auth, @RequestBody @Valid UpdateUserRequest req) {
        UserModel user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado"));
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

    @Operation(summary = "Altera a senha do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody @Valid ChangePasswordRequest req) {
        UserModel user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado"));
        }

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            log.warn("Tentativa de alteração de senha com senha atual incorreta para id={}", user.getId());
            return ResponseEntity.status(400).body(Map.of("message", "Senha atual incorreta"));
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userService.update(user);

        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }

    @Operation(summary = "Exclui a conta do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me")
    public ResponseEntity<?> delete(Authentication auth, @RequestBody @Valid DeleteUserRequest req) {
        UserModel user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado"));
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("message", "Senha incorreta"));
        }

        userService.deleteById(user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Busca usuário por email no formato de ambiente", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/by-email")
    public ResponseEntity<?> findByEmail(@RequestParam String email) {
        return userService.findByEmail(email)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserEnvDTO.from(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado")));
    }

    @Operation(summary = "Lista os ambientes vinculados ao usuário", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{userId}/environments")
    public ResponseEntity<List<UserEnvironmentDTO>> listEnvironments(
            @PathVariable Integer userId,
            Authentication auth) {
        return ResponseEntity.ok(environmentService.listSummariesForUser(userId, auth.getName()));
    }

    @Operation(summary = "Busca usuário por email", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        return userService.findByEmail(email)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResponseDTO.from(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Usuário não encontrado")));
    }
}
