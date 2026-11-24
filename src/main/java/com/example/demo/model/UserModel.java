package com.example.demo.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EnvironmentModel> ownedEnvironments = new ArrayList<>();

    @ManyToMany(mappedBy = "users")
    private List<EnvironmentModel> environments = new ArrayList<>();

    @ManyToMany(mappedBy = "users")
    private List<CardModel> cards = new ArrayList<>();

    public UserModel() {
    }

    public UserModel(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public UserModel(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<EnvironmentModel> getOwnedEnvironments() {
        return ownedEnvironments;
    }

    public void setOwnedEnvironments(List<EnvironmentModel> ownedEnvironments) {
        this.ownedEnvironments = ownedEnvironments;
    }

    public List<EnvironmentModel> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<EnvironmentModel> environments) {
        this.environments = environments;
    }

    public List<CardModel> getCards() {
        return cards;
    }

    public void setCards(List<CardModel> cards) {
        this.cards = cards;
    }
}
