package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Senha é obrigatória")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EnvironmentModel> ownedEnvironments = new ArrayList<>();

    @ManyToMany(mappedBy = "users")
    private List<EnvironmentModel> environments = new ArrayList<>();

    @ManyToMany(mappedBy = "users")
    private List<CardModel> cards = new ArrayList<>();

    public UserModel(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public UserModel(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
