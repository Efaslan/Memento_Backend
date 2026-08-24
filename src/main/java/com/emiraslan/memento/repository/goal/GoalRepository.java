package com.emiraslan.memento.repository.goal;

import com.emiraslan.memento.entity.Goal;
import com.emiraslan.memento.enums.GoalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Finds the latest NOT_DONE GoalLog, and returns the count of GoalLogs that have been logged as not NOT_DONE, after the latest NOT_DONE log
    // '2026-01-01' date is a fallback in case the user ALWAYS completed their goal and NOT_DONE doesn't exist. This means we just need to return the entire count as streak
    @Query(value = """
        SELECT g.goal_id AS goalId,
               (
                   SELECT COUNT(*)
                   FROM goal_logs gl
                   WHERE gl.goal_id = g.goal_id
                     AND gl.status != 'NOT_DONE'
                     AND gl.created_at > COALESCE(
                         (SELECT MAX(created_at) FROM goal_logs gl2 WHERE gl2.goal_id = g.goal_id AND gl2.status = 'NOT_DONE'),
                         '2026-01-01'
                     )
               ) AS streak
        FROM goals g
        WHERE g.patient_user_id = :patientId AND g.is_active = true
    """, nativeQuery = true)
    List<GoalStreakProjection> findActiveGoalStreaks(@Param("patientId") Integer patientId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO goal_logs(goal_id, status, progress_value, log_target_value, log_unit, created_at)
            SELECT g.goal_id, 'NOT_DONE', 0.0, g.current_target_value, g.unit, :yesterday FROM goals g
            WHERE g.is_active = true AND NOT EXISTS (SELECT 1 FROM goal_logs gl WHERE gl.goal_id = g.goal_id AND gl.created_at = :yesterday)
            """, nativeQuery = true)
    int bulkInsertNotDoneGoals(@Param("yesterday") LocalDate yesterday);
}
