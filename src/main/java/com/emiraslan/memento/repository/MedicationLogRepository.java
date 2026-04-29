package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Integer> {

    List<MedicationLog> findByPatient_UserId(Integer patientId);

    // for finding a patient's logs between given dates
    List<MedicationLog> findByPatient_UserIdAndTakenAtBetweenOrderByTakenAtDesc(
            Integer patientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Medical history of a patient between given dates
    List<MedicationLog> findByPatient_UserIdAndTakenAtGreaterThanEqualAndTakenAtLessThan(Integer patientId, LocalDateTime start, LocalDateTime end);

    // Checks if there is a log assigned to a medication schedule. Used to determine if a doctor can edit the schedule or not
    // Relationship chain: Log -> Time -> Schedule -> ScheduleId
    boolean existsByScheduleTime_Schedule_ScheduleId(Integer scheduleId);
}