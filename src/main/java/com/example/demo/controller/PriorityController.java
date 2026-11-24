package com.example.demo.controller;

import com.example.demo.dto.PriorityCreateWrapperDTO;
import com.example.demo.dto.PriorityRequestDTO;
import com.example.demo.dto.PriorityResponseDTO;
import com.example.demo.service.PriorityService;
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
@Tag(name = "Prioridades", description = "Gerenciamento das prioridades de um ambiente")
public class PriorityController {

    private final PriorityService priorityService;

    @Operation(summary = "Lista as prioridades de um ambiente")
    @GetMapping({
            "/environments/{envId}/priority",
            "/environments/{envId}/priorities",
            "/environment/{envId}/priority",
            "/environment/{envId}/priorities"
    })
    public ResponseEntity<List<PriorityResponseDTO>> list(
            @PathVariable Integer envId,
            Authentication auth) {
        return ResponseEntity.ok(priorityService.listByEnvironment(envId, auth.getName()));
    }

    @Operation(summary = "Busca uma prioridade pelo ID")
    @GetMapping({"/priority/{id}", "/priorities/{id}"})
    public ResponseEntity<PriorityResponseDTO> getById(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(priorityService.getById(id, auth.getName()));
    }

    @Operation(summary = "Cria uma nova prioridade no ambiente")
    @PostMapping({
            "/environments/{envId}/priority",
            "/environments/{envId}/priorities",
            "/environment/{envId}/priority",
            "/environment/{envId}/priorities"
    })
    public ResponseEntity<PriorityResponseDTO> create(
            @PathVariable Integer envId,
            @RequestBody @Valid PriorityCreateWrapperDTO body,
            Authentication auth) {
        PriorityResponseDTO created = priorityService.create(envId, body.unwrap(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Atualiza uma prioridade")
    @PutMapping({"/priority/{id}", "/priorities/{id}"})
    public ResponseEntity<PriorityResponseDTO> update(
            @PathVariable Integer id,
            @RequestBody @Valid PriorityRequestDTO body,
            Authentication auth) {
        return ResponseEntity.ok(priorityService.update(id, body, auth.getName()));
    }

    @Operation(summary = "Exclui uma prioridade e limpa referências nos cards")
    @DeleteMapping({"/priority/{id}", "/priorities/{id}"})
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication auth) {
        priorityService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
