package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.Alert;
import com.emiraslan.memento.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    // All alerts of a patient, ordered from latest on top
    List<Alert> findByPatient_UserIdOrderByAlertTimestampDesc(Integer patientId);

    // Finding alerts by their status of: PENDING, SENT, or ACKNOWLEDGED
    List<Alert> findByStatus(AlertStatus status);
}