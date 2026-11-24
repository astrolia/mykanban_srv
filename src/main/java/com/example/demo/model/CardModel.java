package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
public class CardModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Título do card é obrigatório")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer position = 0;

    private LocalDate finalDate;

    @NotNull
    @DecimalMin(value = "0.0", message = "Progresso deve estar entre 0 e 100")
    @DecimalMax(value = "100.0", message = "Progresso deve estar entre 0 e 100")
    @Column(nullable = false)
    private Float progress = 0.0f;

    @NotNull
    @Column(nullable = false)
    private Boolean done = false;

    @NotNull
    @Column(nullable = false)
    private Boolean archived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentModel environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id")
    private ColumnModel column;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_id")
    private PriorityModel priority;

    @ManyToMany
    @JoinTable(
            name = "card_users",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserModel> users = new ArrayList<>();
}
