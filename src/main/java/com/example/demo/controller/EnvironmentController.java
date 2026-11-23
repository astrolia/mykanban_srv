package com.example.demo.controller;

import com.example.demo.dto.EnvironmentArchiveRequestDTO;
import com.example.demo.dto.EnvironmentRequestDTO;
import com.example.demo.dto.EnvironmentResponseDTO;
import com.example.demo.dto.EnvironmentUpdateRequestDTO;
import com.example.demo.dto.EnvironmentUserRequestDTO;
import com.example.demo.dto.UserEnvDTO;
import com.example.demo.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/environment", "/environments"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ambientes", description = "Gerenciamento de ambientes e membros")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @Operation(summary = "Lista todos os ambientes do usuário autenticado")
    @GetMapping
    public ResponseEntity<List<EnvironmentResponseDTO>> list(Authentication auth) {
        return ResponseEntity.ok(environmentService.listForUser(auth.getName()));
    }

    @Operation(summary = "Cria um novo ambiente")
    @PostMapping({"", "/create"})
    public ResponseEntity<EnvironmentResponseDTO> create(
            @RequestBody @Valid EnvironmentRequestDTO req,
            Authentication auth) {
        EnvironmentResponseDTO created = environmentService.createEnvironment(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Carrega um ambiente pelo ID")
    @GetMapping("/{envId}")
    public ResponseEntity<EnvironmentResponseDTO> getById(
            @PathVariable Integer envId,
            Authentication auth) {
        return ResponseEntity.ok(environmentService.getEnvironment(envId, auth.getName()));
    }

    @Operation(summary = "Atualiza dados de um ambiente")
    @RequestMapping(value = "/{envId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<EnvironmentResponseDTO> update(
            @PathVariable Integer envId,
            @RequestBody @Valid EnvironmentUpdateRequestDTO req,
            Authentication auth) {
        return ResponseEntity.ok(environmentService.updateEnvironment(envId, req, auth.getName()));
    }

    @Operation(summary = "Arquiva ou restaura um ambiente (apenas dono)")
    @PatchMapping("/{envId}/archive-status")
    public ResponseEntity<EnvironmentResponseDTO> updateArchiveStatus(
            @PathVariable Integer envId,
            @RequestBody @Valid EnvironmentArchiveRequestDTO req,
            Authentication auth) {
        return ResponseEntity.ok(environmentService.updateArchiveStatus(envId, req.archived(), auth.getName()));
    }

    @Operation(summary = "Exclui um ambiente definitivamente (apenas dono)")
    @DeleteMapping("/{envId}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer envId,
            Authentication auth) {
        environmentService.deleteEnvironment(envId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Adiciona um usuário ao ambiente (apenas dono)")
    @PatchMapping("/{envId}/users")
    public ResponseEntity<UserEnvDTO> addUser(
            @PathVariable Integer envId,
            @RequestBody @Valid EnvironmentUserRequestDTO req,
            Authentication auth) {
        UserEnvDTO added = environmentService.addUser(envId, req.userId(), auth.getName());
        return ResponseEntity.ok(added);
    }

    @Operation(summary = "Remove um usuário do ambiente (apenas dono)")
    @DeleteMapping("/{envId}/users")
    public ResponseEntity<Void> removeUser(
            @PathVariable Integer envId,
            @RequestBody @Valid EnvironmentUserRequestDTO req,
            Authentication auth) {
        environmentService.removeUser(envId, req.userId(), auth.getName());
        return ResponseEntity.noContent().build();
    }
}
