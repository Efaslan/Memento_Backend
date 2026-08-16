package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.Goal;
import com.emiraslan.memento.enums.GoalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Integer> {

    // brings active goals with their logs of today
    @Query("""
    SELECT g AS goal, l AS todayGoalLog
    FROM Goal g
    LEFT JOIN GoalLog l ON l.goal.goalId = g.goalId AND l.createdAt = :today
    WHERE g.patient.userId = :patientId
      AND g.isActive = true
""")
    List<GoalWithTodayLogProjection> findActiveGoalsWithTodayLog(
            @Param("patientId") Integer patientId,
            @Param("today") LocalDate today
    );

    @Query("SELECT s.patient.userId FROM Goal s WHERE s.goalId = :goalId")
    Optional<Integer> findPatientIdByGoalId(Integer goalId);

    // Brings active or deactivated goals
    List<Goal> findByPatient_UserIdAndIsActiveFalse(Integer patientId);

    // For finding if a Goal already exists for known GoalTypes (Water, etc.)
    Optional<Goal> findByPatient_UserIdAndGoalType(Integer patientId, GoalType goalType);
}
