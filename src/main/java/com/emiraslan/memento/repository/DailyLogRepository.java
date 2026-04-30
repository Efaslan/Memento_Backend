package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, Integer> {

    // Patient's logs between given dates (e.g. last week - now, 7 days)
    List<DailyLog> findByPatient_UserIdAndCreatedAtBetween(
            Integer patientId,
            LocalDateTime start,
            LocalDateTime end
    );

    // brings the top 1 record to check if the user created a log today for upsert logic
    Optional<DailyLog> findTopByPatient_UserIdAndCreatedAtBetween(
            Integer patientId,
            LocalDateTime start,
            LocalDateTime end
    );
}