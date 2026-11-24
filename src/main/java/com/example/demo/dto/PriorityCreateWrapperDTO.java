package com.example.demo.dto;

public record PriorityCreateWrapperDTO(PriorityRequestDTO data, String label, String name, String css, String color) {

    public PriorityRequestDTO unwrap() {
        if (data != null) {
            return data;
        }
        return new PriorityRequestDTO(label, name, css, color);
    }
}
