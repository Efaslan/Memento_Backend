package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.AlertRequestDto;
import com.emiraslan.memento.dto.response.AlertResponseDto;
import com.emiraslan.memento.entity.Alert;
import com.emiraslan.memento.entity.PatientRelationship;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.AlertStatus;
import com.emiraslan.memento.repository.AlertRepository;
import com.emiraslan.memento.repository.PatientRelationshipRepository;
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
    private final PatientRelationshipRepository relationshipRepository;
    private final NotificationService notificationService;

    // returns all alerts of a patient
    public List<AlertResponseDto> getPatientAlerts(Integer patientId) {
        return alertRepository.findByPatient_UserIdOrderByAlertTimestampDesc(patientId)
                .stream()
                .map(MapperUtil::toAlertResponseDto)
                .collect(Collectors.toList());
    }

    // immediately creates a PENDING alert when a fall is detected
    @Transactional
    public AlertResponseDto createFallAlert(AlertRequestDto dto, User patient) {

        // Alert is created with PENDING status by default
        Alert alert = MapperUtil.toAlertEntity(dto, patient);

        log.info("Fall Detected (PENDING): PatientID={}, Waiting for mobile confirmation...", patient.getUserId());

        return MapperUtil.toAlertResponseDto(alertRepository.save(alert));
    }

    // if the patient responds within 30 seconds, alert is CANCELLED
    @Transactional
    public AlertResponseDto cancelAlert(Integer alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("ALERT_NOT_FOUND: " + alertId));

        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new IllegalStateException("Only PENDING alerts can be cancelled.");
        }

        alert.setStatus(AlertStatus.CANCELLED);
        log.info("Alert Cancelled by Patient: AlertID={}", alertId);

        return MapperUtil.toAlertResponseDto(alertRepository.save(alert));
    }

    // a relative acknowledges the alert via push notification action
    @Transactional
    public AlertResponseDto acknowledgeAlert(Integer alertId, User caregiver) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("ALERT_NOT_FOUND: " + alertId));

        // Only SENT alerts can be acknowledged
        if (alert.getStatus() != AlertStatus.SENT) {
            throw new IllegalStateException("Alert must be in SENT status to be acknowledged.");
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(caregiver);

        Alert savedAlert = alertRepository.save(alert);
        log.info("Alert Acknowledged by Caregiver: {} (ID: {})", caregiver.getEmail(), caregiver.getUserId());

        // Notify OTHER relatives that someone has acknowledged the alert
        notifyOthersOfAcknowledgment(savedAlert, caregiver);

        return MapperUtil.toAlertResponseDto(savedAlert);
    }

    // Helper method to find active primary contacts and send notifications
    private void notifyPrimaryContacts(Alert alert) {
        // find the patient's primary contacts
        List<PatientRelationship> contacts = relationshipRepository
                .findByPatient_UserIdAndIsPrimaryContactTrueAndIsActiveTrue(alert.getPatient().getUserId());

        if (contacts.isEmpty()) {
            log.warn("NO PRIMARY CONTACT FOUND for PatientID={}. No one was notified for the Alert.", alert.getPatient().getUserId());
            return;
        }

        String patientName = alert.getPatient().getFirstName() + " " + alert.getPatient().getLastName();
        String notificationTitle = "ACİL DURUM: Düşme Tespit Edildi!";
        String notificationBody = patientName + " düştü! Konumu görmek ve müdahale etmek için tıklayın.";

        // for every primary contact
        for (PatientRelationship rel : contacts) {
            User caregiver = rel.getCaregiver();
            notificationService.sendNotificationToUser(caregiver.getUserId(), notificationTitle, notificationBody);
            log.info("Fall Notification sent to Caregiver: {}", caregiver.getEmail());
        }
    }

    // notifying OTHER relatives when someone takes responsibility
    private void notifyOthersOfAcknowledgment(Alert alert, User acknowledger) {
        List<PatientRelationship> contacts = relationshipRepository
                .findByPatient_UserIdAndIsPrimaryContactTrueAndIsActiveTrue(alert.getPatient().getUserId());

        String acknowledgerName = acknowledger.getFirstName() + " " + acknowledger.getLastName();
        String title = "Durum Güncellemesi: Müdahale Ediliyor";
        String body = acknowledgerName + " olayla ilgileniyor.";

        for (PatientRelationship rel : contacts) {
            User relative = rel.getCaregiver();

            // Do not send notification to the person who just clicked the button
            if (relative.getUserId().equals(acknowledger.getUserId())) {
                continue;
            }

            notificationService.sendNotificationToUser(relative.getUserId(), title, body);
            log.info("Acknowledgment info sent to other relative: {}", relative.getEmail());
        }
    }
}