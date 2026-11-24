package com.example.demo.dto;

import java.util.List;

public record CardCreateWrapperDTO(
        CardRequestDTO data,
        String label,
        String name,
        Integer column,
        String description,
        Integer priority,
        Float progress,
        String deadline,
        String finalDate,
        List<Integer> users,
        Boolean done,
        Boolean archived,
        Integer position
) {
    public CardRequestDTO unwrap() {
        if (data != null) {
            return data;
        }
        return new CardRequestDTO(label, name, column, description, priority, progress,
                deadline, finalDate, users, done, archived, position);
    }
}
