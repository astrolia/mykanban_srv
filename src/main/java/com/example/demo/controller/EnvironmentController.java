package com.example.demo.controller;

import com.example.demo.dto.EnvironmentRequestDTO;
import com.example.demo.dto.EnvironmentResponseDTO;
import com.example.demo.dto.EnvironmentUserRequestDTO;
import com.example.demo.dto.UserEnvDTO;
import com.example.demo.service.EnvironmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/environment", "/environments"})
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GetMapping
    public ResponseEntity<List<EnvironmentResponseDTO>> list(Authentication auth) {
        return ResponseEntity.ok(environmentService.listForUser(auth.getName()));
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<EnvironmentResponseDTO> create(
            @RequestBody EnvironmentRequestDTO req,
            Authentication auth) {
        EnvironmentResponseDTO created = environmentService.createEnvironment(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{envId}")
    public ResponseEntity<EnvironmentResponseDTO> getById(
            @PathVariable Integer envId,
            Authentication auth) {
        return ResponseEntity.ok(environmentService.getEnvironment(envId, auth.getName()));
    }

    @RequestMapping(value = "/{envId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<EnvironmentResponseDTO> update(
            @PathVariable Integer envId,
            @RequestBody EnvironmentRequestDTO req,
            Authentication auth) {
        return ResponseEntity.ok(environmentService.updateEnvironment(envId, req, auth.getName()));
    }

    @PatchMapping("/{envId}/archive-status")
    public ResponseEntity<EnvironmentResponseDTO> setArchiveStatus(
            @PathVariable Integer envId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Boolean archived = body != null && body.get("archived") != null
                ? Boolean.valueOf(body.get("archived").toString())
                : false;
        return ResponseEntity.ok(environmentService.setArchiveStatus(envId, archived, auth.getName()));
    }

    @DeleteMapping("/{envId}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer envId,
            Authentication auth) {
        environmentService.deleteEnvironment(envId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{envId}/users")
    public ResponseEntity<UserEnvDTO> addUser(
            @PathVariable Integer envId,
            @RequestBody EnvironmentUserRequestDTO req,
            Authentication auth) {
        UserEnvDTO added = environmentService.addUser(envId, req != null ? req.userId() : null, auth.getName());
        return ResponseEntity.ok(added);
    }

    @DeleteMapping("/{envId}/users")
    public ResponseEntity<Void> removeUser(
            @PathVariable Integer envId,
            @RequestBody EnvironmentUserRequestDTO req,
            Authentication auth) {
        environmentService.removeUser(envId, req != null ? req.userId() : null, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
