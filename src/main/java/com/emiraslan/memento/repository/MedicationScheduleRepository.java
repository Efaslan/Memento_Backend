package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Integer> {

    // Brings all active medication a patient is taking
    List<MedicationSchedule> findByPatient_UserIdAndIsActiveTrue(Integer patientId);

    // All medication assigned to a patient(includes previous and is_active = 0)
    List<MedicationSchedule> findByPatient_UserId(Integer patientId);

    // All PRN(as needed) medication a patient is taking
    List<MedicationSchedule> findByPatient_UserIdAndIsActiveTrueAndIsPrnTrue(Integer patientId);

    // All medication a specific doctor has prescribed
    List<MedicationSchedule> findByDoctor_UserId(Integer doctorId);
}