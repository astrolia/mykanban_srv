package com.example.demo.model;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserModel, Integer> {

    Optional<UserModel> findByEmail(String email);
}