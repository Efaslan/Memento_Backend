package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.Alert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    @Query("SELECT a FROM Alert a JOIN FETCH a.patient WHERE a.alertId = :alertId")
    @EntityGraph(attributePaths = {"patient"})
    Optional<Alert> findByIdWithPatient(Integer alertId);

    // All alerts of a patient, ordered from latest on top
    List<Alert> findByPatient_UserIdOrderByAlertTimestampDesc(Integer patientId);
}