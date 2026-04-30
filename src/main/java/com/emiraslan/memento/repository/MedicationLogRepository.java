package com.emiraslan.memento.repository;

import com.emiraslan.memento.dto.response.MedicationLogSummaryResponseDto;
import com.emiraslan.memento.entity.MedicationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Integer> {

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN m.status = 'TAKEN' THEN 1 ELSE 0 END), 0) AS takenCount,
            COALESCE(SUM(CASE WHEN m.status = 'DELAYED' THEN 1 ELSE 0 END), 0) AS delayedCount,
            COALESCE(SUM(CASE WHEN m.status = 'SKIPPED' THEN 1 ELSE 0 END), 0) AS skippedCount
        FROM MedicationLog m
        WHERE m.patient.userId = :patientId
          AND m.takenAt BETWEEN :start AND :end
    """)
    MedicationLogSummaryResponseDto.StatsProjection getStatistics(
            @Param("patientId") Integer patientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // join fetch with pagination causes OOM, that is why we use EntityGraph
    // pulling schedule data with attributePaths
    @EntityGraph(attributePaths = {"scheduleTime.schedule"})
    Page<MedicationLog> findByPatient_UserIdAndTakenAtBetween(
            Integer patientId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    // Checks if there is a log assigned to a medication schedule. Used to determine if a doctor can edit the schedule or not
    // Relationship chain: Log -> Time -> Schedule.id
    boolean existsByScheduleTime_Schedule_ScheduleId(Integer scheduleId);
}