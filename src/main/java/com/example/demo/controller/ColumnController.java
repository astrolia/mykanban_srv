package com.example.demo.controller;

import com.example.demo.dto.ColumnCreateWrapperDTO;
import com.example.demo.dto.ColumnRequestDTO;
import com.example.demo.dto.ColumnResponseDTO;
import com.example.demo.service.ColumnService;
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
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Colunas", description = "Gerenciamento das colunas do quadro Kanban")
public class ColumnController {

    private final ColumnService columnService;

    @Operation(summary = "Lista as colunas de um ambiente")
    @GetMapping({"/environments/{envId}/columns", "/environment/{envId}/columns"})
    public ResponseEntity<List<ColumnResponseDTO>> list(
            @PathVariable Integer envId,
            Authentication auth) {
        return ResponseEntity.ok(columnService.listByEnvironment(envId, auth.getName()));
    }

    @Operation(summary = "Busca uma coluna pelo ID")
    @GetMapping("/columns/{id}")
    public ResponseEntity<ColumnResponseDTO> getById(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(columnService.getById(id, auth.getName()));
    }

    @Operation(summary = "Cria uma nova coluna no ambiente")
    @PostMapping({"/environments/{envId}/columns", "/environment/{envId}/columns"})
    public ResponseEntity<ColumnResponseDTO> create(
            @PathVariable Integer envId,
            @RequestBody @Valid ColumnCreateWrapperDTO body,
            Authentication auth) {
        ColumnResponseDTO created = columnService.create(envId, body.unwrap(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Atualiza nome ou posição de uma coluna")
    @PutMapping("/columns/{id}")
    public ResponseEntity<ColumnResponseDTO> update(
            @PathVariable Integer id,
            @RequestBody @Valid ColumnRequestDTO body,
            Authentication auth) {
        return ResponseEntity.ok(columnService.update(id, body, auth.getName()));
    }

    @Operation(summary = "Exclui uma coluna e retorna as colunas restantes")
    @DeleteMapping("/columns/{id}")
    public ResponseEntity<List<ColumnResponseDTO>> delete(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(columnService.delete(id, auth.getName()));
    }
}
