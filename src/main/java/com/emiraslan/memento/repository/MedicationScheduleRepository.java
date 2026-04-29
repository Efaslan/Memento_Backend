package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Integer> {

    // Brings all active medication a patient is taking
    List<MedicationSchedule> findByPatient_UserIdAndIsActiveTrue(Integer patientId);

    // All past medication assigned to a patient
    List<MedicationSchedule> findByPatient_UserIdAndIsActiveFalse(Integer patientId);

    // All PRN(as needed) medication a patient is taking
    List<MedicationSchedule> findByPatient_UserIdAndIsActiveTrueAndIsPrnTrue(Integer patientId);

    // For CRON job automatic deactivation of a schedule upon endDate
    List<MedicationSchedule> findByIsActiveTrueAndEndDateBefore(LocalDate date);
}