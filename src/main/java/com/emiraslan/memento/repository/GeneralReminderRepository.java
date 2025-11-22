package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.GeneralReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneralReminderRepository extends JpaRepository<GeneralReminder, Integer> {

    // All ongoing(not completed) reminders of a patient
    List<GeneralReminder> findByPatient_UserIdAndIsCompletedFalseOrderByReminderTimeAsc(Integer patientId);

    // All(complete and incomplete) reminders of a patient.
    List<GeneralReminder> findByPatient_UserIdOrderByReminderTimeAsc(Integer patientId);
    // takvimde eski event'leri gostermek icin bir buton bunu cagirir.
}