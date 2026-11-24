package com.example.demo.controller;

import com.example.demo.dto.CardArchiveRequestDTO;
import com.example.demo.dto.CardCreateWrapperDTO;
import com.example.demo.dto.CardMoveRequestDTO;
import com.example.demo.dto.CardRequestDTO;
import com.example.demo.dto.CardResponseDTO;
import com.example.demo.dto.CardRestoreRequestDTO;
import com.example.demo.service.CardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping({"/environments/{envId}/cards", "/environment/{envId}/cards"})
    public ResponseEntity<List<CardResponseDTO>> list(
            @PathVariable Integer envId,
            @RequestParam(required = false) Boolean backlog,
            @RequestParam(required = false) Boolean archived,
            Authentication auth) {
        return ResponseEntity.ok(cardService.listByEnvironment(envId, backlog, archived, auth.getName()));
    }

    @GetMapping({"/environments/{envId}/cards/archived", "/environment/{envId}/cards/archived"})
    public ResponseEntity<List<CardResponseDTO>> listArchived(
            @PathVariable Integer envId,
            Authentication auth) {
        return ResponseEntity.ok(cardService.listArchivedByEnvironment(envId, auth.getName()));
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<CardResponseDTO> getById(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(cardService.getById(id, auth.getName()));
    }

    @PostMapping({"/environments/{envId}/cards", "/environment/{envId}/cards"})
    public ResponseEntity<CardResponseDTO> create(
            @PathVariable Integer envId,
            @RequestBody CardCreateWrapperDTO body,
            Authentication auth) {
        CardResponseDTO created = cardService.create(envId, body != null ? body.unwrap() : null, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @RequestBody CardRequestDTO body,
            Authentication auth) {
        return ResponseEntity.ok(cardService.update(id, body, auth.getName()));
    }

    @RequestMapping(
            value = {"/cards/{id}/archive", "/cards/archive/{id}"},
            method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH}
    )
    public ResponseEntity<List<CardResponseDTO>> archive(
            @PathVariable Integer id,
            @RequestBody(required = false) CardArchiveRequestDTO body,
            Authentication auth) {
        Boolean done = body != null ? body.done() : null;
        return ResponseEntity.ok(cardService.archiveCard(id, done, auth.getName()));
    }

    @RequestMapping(
            value = {"/cards/{id}/restore", "/cards/restore/{id}"},
            method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH}
    )
    public ResponseEntity<List<CardResponseDTO>> restore(
            @PathVariable Integer id,
            @RequestBody(required = false) CardRestoreRequestDTO body,
            Authentication auth) {
        Integer column = body != null ? body.column() : null;
        return ResponseEntity.ok(cardService.restoreCard(id, column, auth.getName()));
    }

    @PutMapping("/cards/move/{id}")
    public ResponseEntity<List<CardResponseDTO>> move(
            @PathVariable Integer id,
            @RequestBody CardMoveRequestDTO body,
            Authentication auth) {
        return ResponseEntity.ok(cardService.move(id, body, auth.getName()));
    }

    @DeleteMapping("/cards/{id}/permanent")
    public ResponseEntity<Void> deletePermanent(
            @PathVariable Integer id,
            Authentication auth) {
        cardService.deletePermanent(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            @RequestParam(required = false) Boolean permanent,
            Authentication auth) {
        cardService.delete(id, permanent, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
