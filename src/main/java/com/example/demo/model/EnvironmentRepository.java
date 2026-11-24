package com.example.demo.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvironmentRepository extends JpaRepository<EnvironmentModel, Integer> {

    List<EnvironmentModel> findDistinctByOwnerOrUsersContainingOrderByPositionAscIdAsc(UserModel owner, UserModel user);

    List<EnvironmentModel> findByUsersContaining(UserModel user);
}
