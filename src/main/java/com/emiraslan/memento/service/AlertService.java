package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.AlertDto;
import com.emiraslan.memento.entity.Alert;
import com.emiraslan.memento.entity.PatientRelationship;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.AlertStatus;
import com.emiraslan.memento.repository.AlertRepository;
import com.emiraslan.memento.repository.PatientRelationshipRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final PatientRelationshipRepository relationshipRepository;

    // immediately creates a PENDING alert when a fall is detected
    @Transactional
    public AlertDto createAlert(AlertDto dto) {
        User patient = userRepository.findById(dto.getPatientUserId())
                .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND: " + dto.getPatientUserId()));

        // Alert is created with PENDING status by default
        Alert alert = MapperUtil.toAlertEntity(dto, patient);

        log.info("Fall Detected (PENDING): PatientID={}, Waiting for mobile confirmation...", patient.getUserId());

        return MapperUtil.toAlertDto(alertRepository.save(alert));
    }

    // if the patient responds within 30 seconds, alert is CANCELLED
    @Transactional
    public AlertDto cancelAlert(Integer alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("ALERT_NOT_FOUND: " + alertId));

        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new IllegalStateException("Only PENDING alerts can be cancelled.");
        }

        alert.setStatus(AlertStatus.CANCELLED);
        log.info("Alert Cancelled by Patient: AlertID={}", alertId);

        return MapperUtil.toAlertDto(alertRepository.save(alert));
    }

    // if 30 seconds pass without a response, notifications are sent to primary contacts and status is set to SENT
    @Transactional
    public AlertDto confirmFallAlert(Integer alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("ALERT_NOT_FOUND: " + alertId));

        // We can only confirm pending alerts
        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new IllegalStateException("Alert is not in PENDING state, cannot send.");
        }

        // Update status to SENT
        alert.setStatus(AlertStatus.SENT);
        Alert savedAlert = alertRepository.save(alert);

        log.info("Alert Confirmed by Mobile (Timeout): AlertID={} -> Status: SENT", alertId);

        // Send notifications to primary contacts
        notifyPrimaryContacts(savedAlert);

        return MapperUtil.toAlertDto(savedAlert);
    }

    // a relative acknowledges the alert via push notification action
    @Transactional
    public AlertDto acknowledgeAlert(Integer alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("ALERT_NOT_FOUND: " + alertId));

        // Only SENT alerts can be acknowledged
        if (alert.getStatus() != AlertStatus.SENT) {
            throw new IllegalStateException("Alert must be in SENT status to be acknowledged.");
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        log.info("Alert Acknowledged by Caregiver: AlertID={}", alertId);

        // TODO: Notify other relatives that someone has acknowledged the alert (prevent duplicate efforts)

        return MapperUtil.toAlertDto(alertRepository.save(alert));
    }

    // returns all alerts of a patient
    public List<AlertDto> getPatientAlerts(Integer patientId) {
        return alertRepository.findByPatient_UserIdOrderByAlertTimestampDesc(patientId)
                .stream()
                .map(MapperUtil::toAlertDto)
                .collect(Collectors.toList());
    }

    // Helper method to find active primary contacts and send notifications
    private void notifyPrimaryContacts(Alert alert) {
        List<PatientRelationship> contacts = relationshipRepository
                .findByPatient_UserIdAndIsPrimaryContactTrueAndIsActiveTrue(alert.getPatient().getUserId());

        if (contacts.isEmpty()) {
            log.warn("NO PRIMARY CONTACT FOUND for PatientID={}. No one was notified for the Alert.", alert.getPatient().getUserId());
            return;
        }

        for (PatientRelationship rel : contacts) {
            // Todo: Call FCM Service here later
            log.info(">>> SENDING PUSH NOTIFICATION to Caregiver: {} (Email: {}) - Message: FALL DETECTED at Lat:{}, Lng:{}",
                    rel.getCaregiver().getFirstName(),
                    rel.getCaregiver().getEmail(),
                    alert.getLatitude(),
                    alert.getLongitude()
            );
        }
    }
}