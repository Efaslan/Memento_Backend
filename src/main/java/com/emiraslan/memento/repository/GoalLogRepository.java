package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.GoalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GoalLogRepository extends JpaRepository<GoalLog, Integer> {

    // Upserting today's log
    Optional<GoalLog> findByGoal_GoalIdAndCreatedAt(Integer goalId, LocalDate createdAt);

    // For bringing a goal's logs of a given timeframe, for example 14 days
    @Query("SELECT l FROM GoalLog l WHERE l.goal.goalId = :goalId AND l.createdAt BETWEEN :start AND :end ORDER BY l.createdAt DESC")
    List<GoalLog> findLogsByGoalIdAndDateRange(
            @Param("goalId") Integer goalId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
