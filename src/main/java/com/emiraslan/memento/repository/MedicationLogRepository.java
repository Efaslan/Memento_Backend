package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Integer> {

    // Medical history of a patient between given dates
    List<MedicationLog> findByPatient_UserIdAndTakenAtBetween(Integer patientId, LocalDateTime start, LocalDateTime end);

    // Checks if a medicine that has a time is taken between given dates. (Scenario: the 10:00 am medicine of today.)
    boolean existsByScheduleTime_TimeIdAndTakenAtBetween(Integer scheduleTimeId, LocalDateTime start, LocalDateTime end);

    // Checks if there is a log assigned to a medication schedule. Used to determine if a doctor can edit the schedule or not
    // Relationship chain: Log -> Time -> Schedule -> ScheduleId
    boolean existsByScheduleTime_Schedule_ScheduleId(Integer scheduleId);
}