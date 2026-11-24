package com.example.demo.dto;

public record ColumnCreateWrapperDTO(ColumnRequestDTO column, String label, String name, Integer position) {

    public ColumnRequestDTO unwrap() {
        if (column != null) {
            return column;
        }
        return new ColumnRequestDTO(label, name, position);
    }
}
