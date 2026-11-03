package com.example.demo.model;

import com.example.demo.model.enums.ColumnsDefault;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CardModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private Integer position;
    private String description;
    @ManyToOne
    @JoinColumn(name = "column_id")
    private ColumnModel column;
}
