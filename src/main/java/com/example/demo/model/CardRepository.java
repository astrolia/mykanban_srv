package com.example.demo.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<CardModel, Integer> {

    List<CardModel> findByEnvironmentIdOrderByPositionAscIdAsc(Integer environmentId);

    List<CardModel> findByEnvironmentIdAndArchivedTrueOrderByPositionAscIdAsc(Integer environmentId);

    List<CardModel> findByEnvironmentIdAndArchivedFalseOrderByPositionAscIdAsc(Integer environmentId);

    List<CardModel> findByColumnId(Integer columnId);

    List<CardModel> findByColumnIdOrderByPositionAscIdAsc(Integer columnId);

    List<CardModel> findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(Integer columnId);

    List<CardModel> findByEnvironmentIdAndColumnIsNullAndArchivedFalseOrderByPositionAscIdAsc(Integer environmentId);

    boolean existsByColumnId(Integer columnId);

    boolean existsByColumnIdAndArchivedFalse(Integer columnId);

    List<CardModel> findByPriorityId(Integer priorityId);

    List<CardModel> findByEnvironmentIdAndUsersContaining(Integer environmentId, UserModel user);

    List<CardModel> findByUsersContaining(UserModel user);
}
