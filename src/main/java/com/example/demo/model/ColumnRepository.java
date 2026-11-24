package com.example.demo.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColumnRepository extends JpaRepository<ColumnModel, Integer> {

    List<ColumnModel> findByEnvironmentIdOrderByPositionAscIdAsc(Integer environmentId);

    boolean existsByEnvironmentIdAndNameIgnoreCase(Integer environmentId, String name);

    boolean existsByEnvironmentIdAndNameIgnoreCaseAndIdNot(Integer environmentId, String name, Integer id);
}
