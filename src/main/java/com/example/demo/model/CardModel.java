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
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "card_column")
    private ColumnsDefault column;
    @ManyToOne
    @JoinColumn(name = "environment_id")
    private EnvironmentModel environment;
}
