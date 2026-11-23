package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "priorities")
@Getter
@Setter
@NoArgsConstructor
public class PriorityModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Nome da prioridade é obrigatório")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Cor da prioridade é obrigatória")
    @Column(nullable = false)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentModel environment;

    public PriorityModel(String name, String color, EnvironmentModel environment) {
        this.name = name;
        this.color = color;
        this.environment = environment;
    }
}
