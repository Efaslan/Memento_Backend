package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.DailyLog;
import com.emiraslan.memento.enums.DailyLogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, Integer> {

    // Patient's food and water logs between given dates
    List<DailyLog> findByPatient_UserIdAndCreatedAtBetween(Integer patientId, LocalDateTime start, LocalDateTime end);

    // Brings a specific log type(FOOD, or WATER) between given dates
    List<DailyLog> findByPatient_UserIdAndDailyLogTypeAndCreatedAtBetween(Integer patientId, DailyLogType dailyLogType, LocalDateTime start, LocalDateTime end);
}