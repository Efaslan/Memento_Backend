package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.MedicationScheduleTime;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface MedicationScheduleTimeRepository extends JpaRepository<MedicationScheduleTime, Integer> {

    // Brings all times assigned to a prescription
    List<MedicationScheduleTime> findBySchedule_ScheduleId(Integer scheduleId);

    // brings "now" from all times in active schedules
    List<MedicationScheduleTime> findBySchedule_IsActiveTrueAndScheduledTime(LocalTime scheduledTime);

    // Avoiding 1+2n query in service method by join fetching schedule and patient data
    @Query("""
    SELECT mst FROM MedicationScheduleTime mst
    JOIN FETCH mst.schedule s
    JOIN FETCH s.patient
    WHERE s.isActive = true
      AND mst.scheduledTime <= :threshold
      AND NOT EXISTS (
          SELECT 1 FROM MedicationLog ml
          WHERE ml.scheduleTime = mst
            AND ml.takenAt >= :startOfDay
            AND ml.takenAt <= :now
      )
    """)
    List<MedicationScheduleTime> findOverdueTimesWithoutLogsToday(
            @Param("threshold") LocalTime threshold,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("now") LocalDateTime now
    );
}