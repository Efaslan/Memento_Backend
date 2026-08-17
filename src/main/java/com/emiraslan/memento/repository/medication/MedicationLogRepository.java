package com.emiraslan.memento.repository.medication;

import com.emiraslan.memento.entity.medication.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Integer> {

    @Query("SELECT l FROM MedicationLog l " +
            "JOIN l.scheduleTime t " +
            "WHERE t.schedule.scheduleId = :scheduleId " +
            "AND l.takenAt BETWEEN :start AND :end " +
            "ORDER BY l.takenAt DESC")
    List<MedicationLog> findLogsByScheduleIdAndDateRange(
            @Param("scheduleId") Integer scheduleId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT
        COALESCE(SUM(CASE WHEN m.status = 'TAKEN' THEN 1 ELSE 0 END), 0) AS takenCount,
        COALESCE(SUM(CASE WHEN m.status = 'LATE_DOSE' THEN 1 ELSE 0 END), 0) AS delayedCount,
        COALESCE(SUM(CASE WHEN m.status = 'SKIPPED' THEN 1 ELSE 0 END), 0) AS skippedCount,
        COUNT(m.medicationLogId) AS totalLogs
    FROM MedicationLog m
    JOIN m.scheduleTime t
    WHERE t.schedule.patient.userId = :patientId
      AND t.schedule.isActive = :isActive
""")
    ScheduleStatsProjection getOverallStatisticsByPatient(
            @Param("patientId") Integer patientId,
            @Param("isActive") Boolean isActive
    );

    // Checks if there is a log assigned to a medication schedule. Used to determine if the schedule can be edited or not
    // Relationship chain: Log -> Time -> Schedule.id
    boolean existsByScheduleTime_Schedule_ScheduleId(Integer scheduleId);

    boolean existsByScheduleTime_TimeIdAndTakenAtBetween(Integer scheduleTimeId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("""
    SELECT l FROM MedicationLog l
    JOIN FETCH l.scheduleTime t
    WHERE t.schedule.scheduleId IN :scheduleIds
      AND l.takenAt BETWEEN :startOfDay AND :endOfDay
""")
    List<MedicationLog> findTodayLogsByScheduleIds(
            @Param("scheduleIds") List<Integer> scheduleIds,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}