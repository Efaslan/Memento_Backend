package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.Alert;
import com.emiraslan.memento.enums.AlertStatus;
import com.emiraslan.memento.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    // All alerts of a patient, ordered from latest on top
    List<Alert> findByPatient_UserIdOrderByAlertTimestampDesc(Integer patientId);

    // finds a patient's of (status) of (alertType) alerts
    List<Alert> findByPatient_UserIdAndStatusAndAlertType(
            Integer patientUserId,
            AlertStatus status,
            AlertType alertType
    );
}