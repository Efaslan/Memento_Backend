package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.GeneralReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GeneralReminderRepository extends JpaRepository<GeneralReminder, Integer> {

    // All reminders of a patient
    List<GeneralReminder> findByPatient_UserIdOrderByReminderTimeAsc(Integer patientId);

    // Finding reminders and patients for Daily Reminder notification Cron
    @Query("SELECT r FROM GeneralReminder r JOIN FETCH r.patient WHERE r.reminderTime <= :now")
    List<GeneralReminder> findDueRemindersWithPatient(@Param("now") LocalDateTime now);
}