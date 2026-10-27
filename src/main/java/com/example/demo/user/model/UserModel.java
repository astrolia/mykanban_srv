package com.example.demo.user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer Id;
    @Setter
    @Getter
    private String name;
    @Setter
    @Getter
    private String cpf;
    @Setter
    @Getter
    private String password;
    @Setter
    @Getter
    private String email;

    public UserModel(){}

    public UserModel(String name, String cpf, String password, String email){
        this.name = name;
        this.cpf = cpf;
        this.password = password;
        this.email = email;
    }
}
