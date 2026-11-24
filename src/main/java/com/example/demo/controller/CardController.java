package com.example.demo.controller;

import com.example.demo.dto.CardCreateWrapperDTO;
import com.example.demo.dto.CardMoveRequestDTO;
import com.example.demo.dto.CardRequestDTO;
import com.example.demo.dto.CardResponseDTO;
import com.example.demo.service.CardService;
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
@Tag(name = "Cards", description = "Gerenciamento, ordenação e arquivamento de cards")
public class CardController {

    private final CardService cardService;

    @Operation(summary = "Lista os cards de um ambiente")
    @GetMapping({"/environments/{envId}/cards", "/environment/{envId}/cards"})
    public ResponseEntity<List<CardResponseDTO>> list(
            @PathVariable Integer envId,
            @RequestParam(required = false) Boolean backlog,
            @RequestParam(required = false) Boolean archived,
            Authentication auth) {
        return ResponseEntity.ok(cardService.listByEnvironment(envId, backlog, archived, auth.getName()));
    }

    @Operation(summary = "Busca um card pelo ID")
    @GetMapping("/cards/{id}")
    public ResponseEntity<CardResponseDTO> getById(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(cardService.getById(id, auth.getName()));
    }

    @Operation(summary = "Cria um novo card no ambiente")
    @PostMapping({"/environments/{envId}/cards", "/environment/{envId}/cards"})
    public ResponseEntity<CardResponseDTO> create(
            @PathVariable Integer envId,
            @RequestBody @Valid CardCreateWrapperDTO body,
            Authentication auth) {
        CardResponseDTO created = cardService.create(envId, body.unwrap(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Atualiza, arquiva ou restaura um card")
    @PutMapping("/cards/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestBody @Valid CardRequestDTO body,
            Authentication auth) {
        return ResponseEntity.ok(cardService.update(id, body, auth.getName()));
    }

    @Operation(summary = "Move ou reordena um card entre colunas")
    @PutMapping("/cards/move/{id}")
    public ResponseEntity<List<CardResponseDTO>> move(
            @PathVariable Integer id,
            @RequestBody @Valid CardMoveRequestDTO body,
            Authentication auth) {
        return ResponseEntity.ok(cardService.move(id, body, auth.getName()));
    }

    @Operation(summary = "Exclui um card definitivamente")
    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication auth) {
        cardService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
