package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.MedicationScheduleTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationScheduleTimeRepository extends JpaRepository<MedicationScheduleTime, Integer> {

    // Brings all times assigned to a prescription
    List<MedicationScheduleTime> findBySchedule_ScheduleId(Integer scheduleId);
}