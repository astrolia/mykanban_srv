package com.example.demo.dto;

import com.example.demo.model.PriorityModel;

public record PriorityResponseDTO(Integer id, String label, String css) {

    public static PriorityResponseDTO from(PriorityModel priority) {
        return new PriorityResponseDTO(priority.getId(), priority.getName(), priority.getColor());
    }
}
