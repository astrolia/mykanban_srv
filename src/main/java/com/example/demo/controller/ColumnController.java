package com.example.demo.controller;

import com.example.demo.dto.ColumnCreateWrapperDTO;
import com.example.demo.dto.ColumnRequestDTO;
import com.example.demo.dto.ColumnResponseDTO;
import com.example.demo.service.ColumnService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ColumnController {

    private final ColumnService columnService;

    public ColumnController(ColumnService columnService) {
        this.columnService = columnService;
    }

    @GetMapping({"/environments/{envId}/columns", "/environment/{envId}/columns"})
    public ResponseEntity<List<ColumnResponseDTO>> list(
            @PathVariable Integer envId,
            Authentication auth) {
        return ResponseEntity.ok(columnService.listByEnvironment(envId, auth.getName()));
    }

    @GetMapping("/columns/{id}")
    public ResponseEntity<ColumnResponseDTO> getById(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(columnService.getById(id, auth.getName()));
    }

    @PostMapping({"/environments/{envId}/columns", "/environment/{envId}/columns"})
    public ResponseEntity<ColumnResponseDTO> create(
            @PathVariable Integer envId,
            @RequestBody ColumnCreateWrapperDTO body,
            Authentication auth) {
        ColumnResponseDTO created = columnService.create(envId, body != null ? body.unwrap() : null, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/columns/{id}")
    public ResponseEntity<ColumnResponseDTO> update(
            @PathVariable Integer id,
            @RequestBody ColumnRequestDTO body,
            Authentication auth) {
        return ResponseEntity.ok(columnService.update(id, body, auth.getName()));
    }

    @DeleteMapping("/columns/{id}")
    public ResponseEntity<List<ColumnResponseDTO>> delete(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(columnService.delete(id, auth.getName()));
    }
}
