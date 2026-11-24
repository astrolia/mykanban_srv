package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board_columns")
@Getter
@Setter
@NoArgsConstructor
public class ColumnModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Nome da coluna é obrigatório")
    @Column(nullable = false)
    private String name;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer position = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentModel environment;

    @OneToMany(mappedBy = "column")
    private List<CardModel> cards = new ArrayList<>();

    public ColumnModel(String name, Integer position, EnvironmentModel environment) {
        this.name = name;
        this.position = position;
        this.environment = environment;
    }
}
