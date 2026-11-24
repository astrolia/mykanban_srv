package com.example.demo.controller;

import com.example.demo.dto.PriorityCreateWrapperDTO;
import com.example.demo.dto.PriorityRequestDTO;
import com.example.demo.dto.PriorityResponseDTO;
import com.example.demo.service.PriorityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PriorityController {

    private final PriorityService priorityService;

    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

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

    @GetMapping({"/priority/{id}", "/priorities/{id}"})
    public ResponseEntity<PriorityResponseDTO> getById(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(priorityService.getById(id, auth.getName()));
    }

    @PostMapping({
            "/environments/{envId}/priority",
            "/environments/{envId}/priorities",
            "/environment/{envId}/priority",
            "/environment/{envId}/priorities"
    })
    public ResponseEntity<PriorityResponseDTO> create(
            @PathVariable Integer envId,
            @RequestBody PriorityCreateWrapperDTO body,
            Authentication auth) {
        PriorityResponseDTO created = priorityService.create(envId, body != null ? body.unwrap() : null, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping({"/priority/{id}", "/priorities/{id}"})
    public ResponseEntity<PriorityResponseDTO> update(
            @PathVariable Integer id,
            @RequestBody PriorityRequestDTO body,
            Authentication auth) {
        return ResponseEntity.ok(priorityService.update(id, body, auth.getName()));
    }

    @DeleteMapping({"/priority/{id}", "/priorities/{id}"})
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication auth) {
        priorityService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
