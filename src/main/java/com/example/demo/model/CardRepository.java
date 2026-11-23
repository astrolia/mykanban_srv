package com.example.demo.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<CardModel, Integer> {

    List<CardModel> findByEnvironmentIdOrderByPositionAscIdAsc(Integer environmentId);

    List<CardModel> findByColumnIdOrderByPositionAscIdAsc(Integer columnId);

    List<CardModel> findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(Integer columnId);

    List<CardModel> findByColumnIdAndArchivedTrue(Integer columnId);

    List<CardModel> findByEnvironmentIdAndColumnIsNullAndArchivedFalseOrderByPositionAscIdAsc(Integer environmentId);

    boolean existsByColumnIdAndArchivedFalse(Integer columnId);

    List<CardModel> findByPriorityId(Integer priorityId);

    List<CardModel> findByEnvironmentIdAndUsersContaining(Integer environmentId, UserModel user);

    List<CardModel> findByUsersContaining(UserModel user);
}
