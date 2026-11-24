package com.example.demo.dto;

import com.example.demo.model.CardModel;
import com.example.demo.model.UserModel;

import java.time.LocalDate;
import java.util.List;

public record CardResponseDTO(
        Integer id,
        String label,
        Integer column,
        String description,
        Integer priority,
        Float progress,
        LocalDate deadline,
        List<Integer> users,
        Boolean done,
        Boolean archived,
        Integer position
) {
    public static CardResponseDTO from(CardModel card) {
        return new CardResponseDTO(
                card.getId(),
                card.getName(),
                card.getColumn() != null ? card.getColumn().getId() : null,
                card.getDescription(),
                card.getPriority() != null ? card.getPriority().getId() : null,
                card.getProgress(),
                card.getFinalDate(),
                card.getUsers() != null
                        ? card.getUsers().stream().map(UserModel::getId).toList()
                        : List.of(),
                card.getDone(),
                card.getArchived(),
                card.getPosition()
        );
    }
}
