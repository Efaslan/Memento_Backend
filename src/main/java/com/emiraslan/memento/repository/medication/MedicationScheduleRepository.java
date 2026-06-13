package com.emiraslan.memento.repository.medication;

import com.emiraslan.memento.entity.medication.MedicationSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Integer> {

    // Brings all active medication a patient is taking
    List<MedicationSchedule> findByPatient_UserIdAndIsActiveTrue(Integer patientId);

    // All past medication assigned to a patient
    Page<MedicationSchedule> findByPatient_UserIdAndIsActiveFalse(Integer patientId, Pageable pageable);

    // For CRON job automatic deactivation of a schedule upon endDate
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MedicationSchedule m SET m.isActive = false WHERE m.isActive = true AND m.endDate < :today")
    int deactivateExpiredSchedules(@Param("today") LocalDate today);

    @Query("SELECT s.patient.userId FROM MedicationSchedule s WHERE s.scheduleId = :scheduleId")
    Optional<Integer> findPatientIdByScheduleId(@Param("scheduleId") Integer scheduleId);
}