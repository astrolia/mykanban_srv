package com.example.demo.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriorityRepository extends JpaRepository<PriorityModel, Integer> {

    List<PriorityModel> findByEnvironmentIdOrderByIdAsc(Integer environmentId);

    boolean existsByEnvironmentIdAndNameIgnoreCase(Integer environmentId, String name);

    boolean existsByEnvironmentIdAndNameIgnoreCaseAndIdNot(Integer environmentId, String name, Integer id);
}
