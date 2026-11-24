package com.example.demo.dto;

import com.example.demo.model.ColumnModel;

public record ColumnResponseDTO(Integer id, String label, Integer position) {

    public static ColumnResponseDTO from(ColumnModel column) {
        return new ColumnResponseDTO(column.getId(), column.getName(), column.getPosition());
    }
}
