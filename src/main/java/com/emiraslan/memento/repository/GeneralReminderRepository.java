package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.GeneralReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneralReminderRepository extends JpaRepository<GeneralReminder, Integer> {

    // All ongoing(not completed) reminders of a patient
    List<GeneralReminder> findByPatient_UserIdAndIsCompletedFalseOrderByReminderTimeAsc(Integer patientId);

    // All complete(past) reminders of a patient. Used with mobile's "show past events" toggle
    List<GeneralReminder> findByPatient_UserIdAndIsCompletedTrueOrderByReminderTimeAsc(Integer patientId);
}