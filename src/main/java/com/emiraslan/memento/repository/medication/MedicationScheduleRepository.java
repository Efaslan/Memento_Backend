package com.emiraslan.memento.repository.medication;

import com.emiraslan.memento.entity.medication.MedicationSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Integer> {

    // Brings all active medication a patient is taking
    List<MedicationSchedule> findByPatient_UserIdAndIsActiveTrue(Integer patientId);

    // All past medication assigned to a patient
    Page<MedicationSchedule> findByPatient_UserIdAndIsActiveFalse(Integer patientId, Pageable pageable);

    // For CRON job automatic deactivation of a schedule upon endDate
    List<MedicationSchedule> findByIsActiveTrueAndEndDateBefore(LocalDate date);
}