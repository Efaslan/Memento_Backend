package com.emiraslan.memento.security;

import com.emiraslan.memento.dto.GeneralReminderDto;
import com.emiraslan.memento.dto.MedicationScheduleDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.RelationshipType;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service("guard")
@RequiredArgsConstructor
public class SecurityService {

    private final PatientRelationshipRepository relationshipRepository;
    private final SavedLocationRepository locationRepository;
    private final GeneralReminderRepository reminderRepository;
    private final DailyLogRepository dailyLogRepository;
    private final AlertRepository alertRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final MedicationScheduleTimeRepository timesRepository;

    // --- helper method ----
    private boolean hasActiveRelationship(Integer patientId, Integer caregiverId){
        return relationshipRepository.findByPatient_UserIdAndCaregiver_UserId(patientId, caregiverId)
                .map(rel -> Boolean.TRUE.equals(rel.getIsActive()))
                .orElse(false);
    }

    // mutual method to check for relationships
    public boolean canViewPatientData(Integer patientId, User user) {
        if (patientId.equals(user.getUserId())) return true;

        if (!hasActiveRelationship(patientId, user.getUserId())) {
            throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
        }
        return true;
    }

    // ========================================================================
    // RELATIONSHIP SECURITY
    // ========================================================================

    public boolean canUpdateRelationship(Integer relationshipId, User user) {
        return relationshipRepository.findById(relationshipId)
                .map(rel -> {
                    boolean isPatient = rel.getPatient().getUserId().equals(user.getUserId());
                    boolean isCaregiver = rel.getCaregiver().getUserId().equals(user.getUserId());

                    if (!isPatient && !isCaregiver) {
                        throw new AccessDeniedException("YOU_ARE_NOT_PART_OF_THIS_RELATIONSHIP");
                    }

                    // only doctor users can update type.doctor relationships
                    if (rel.getRelationshipType() == RelationshipType.DOCTOR && !isCaregiver) {
                        throw new AccessDeniedException("ONLY_DOCTORS_CAN_UPDATE_DOCTOR_RELATIONSHIPS");
                    }

                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));
    }

    // ========================================================================
    // SAVED LOCATION SECURITY
    // ========================================================================

    public boolean isLocationOwner(Integer locationId, User user){
        return locationRepository.findById(locationId)
                .map(location -> {
                    if (!location.getPatient().getUserId().equals(user.getUserId())){
                        throw new AccessDeniedException("NOT_LOCATION_OWNER");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("LOCATION_NOT_FOUND"));
    }

    // ========================================================================
    // GENERAL REMINDER SECURITY
    // ========================================================================

    public boolean canCreateReminder(GeneralReminderDto dto, User user) {
        // patients do not need to include id in their request. It is automatically set in service
        if (user.getRole() == UserRole.PATIENT) {
            return true;
        }

        // this will throw 400 error if doctors or relatives did not include patient's id. Does not block patients because of method order
        if (dto.getPatientUserId() == null) {
            throw new IllegalArgumentException("PATIENT_ID_REQUIRED");
        }

        if (!hasActiveRelationship(dto.getPatientUserId(), user.getUserId())) {
            throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
        }
        return true;
    }

    // only the patient, or the creator, can update/delete the reminder
    public boolean canModifyReminder(Integer reminderId, User user) {
        return reminderRepository.findById(reminderId)
                .map(reminder -> {
                    boolean isPatient = reminder.getPatient().getUserId().equals(user.getUserId());
                    boolean isCreator = reminder.getCreator() != null && reminder.getCreator().getUserId().equals(user.getUserId());

                    if (!isPatient && !isCreator) {
                        throw new AccessDeniedException("YOU_CAN_ONLY_MODIFY_YOUR_OWN_OR_CREATED_REMINDERS");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND"));
    }

    // ========================================================================
    // DAILY LOG SECURITY
    // ========================================================================

    public boolean isDailyLogOwner(Integer logId, User user) {
        return dailyLogRepository.findById(logId)
                .map(log -> {
                    if (!log.getPatient().getUserId().equals(user.getUserId())) {
                        throw new AccessDeniedException("NOT_DAILY_LOG_OWNER");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("DAILY_LOG_NOT_FOUND"));
    }

    // ========================================================================
    // ALERT SECURITY
    // ========================================================================

    public boolean isAlertOwner(Integer alertId, User user) {
        return alertRepository.findById(alertId)
                .map(alert -> {
                    if (!alert.getPatient().getUserId().equals(user.getUserId())) {
                        throw new AccessDeniedException("NOT_ALERT_OWNER");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("ALERT_NOT_FOUND"));
    }

    public boolean canAcknowledgeAlert(Integer alertId, User user) {
        // find patient id from alert
        Integer patientId = alertRepository.findById(alertId)
                .map(alert -> alert.getPatient().getUserId())
                .orElseThrow(() -> new EntityNotFoundException("ALERT_NOT_FOUND"));

        // check if the user is a primary contact of the patient
        boolean isPrimaryContact = relationshipRepository
                .existsByPatient_UserIdAndCaregiver_UserIdAndIsPrimaryContactTrueAndIsActiveTrue(patientId, user.getUserId());

        if (!isPrimaryContact) {
            throw new AccessDeniedException("ONLY_PRIMARY_CONTACTS_CAN_ACKNOWLEDGE_ALERTS");
        }
        return true;
    }

    // ========================================================================
    // MEDICATION SECURITY
    // ========================================================================
    public boolean isScheduleTimeOwner(Integer timeId, User user){
        return timesRepository.findById(timeId)
                .map(time -> {
                    if(!time.getSchedule().getPatient().getUserId().equals(user.getUserId())){
                        throw new AccessDeniedException("NOT_YOUR_MEDICATION");
                    }
                    return true;
                }).orElseThrow(() -> new EntityNotFoundException("SCHEDULE_TIME_NOT_FOUND"));
    }

    public boolean canCreateSchedule(MedicationScheduleDto dto, User doctor){
        if (dto.getPatientUserId() == null) throw new IllegalArgumentException("PATIENT_ID_REQUIRED");

        return relationshipRepository.findByPatient_UserIdAndCaregiver_UserId(dto.getPatientUserId(), doctor.getUserId())
                .map(rel -> {
                    if (!Boolean.TRUE.equals(rel.getIsActive())) {
                        throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
                    }
                    if (rel.getRelationshipType() != RelationshipType.DOCTOR) {
                        throw new AccessDeniedException("ONLY_ASSIGNED_DOCTORS_CAN_CREATE_PRESCRIPTIONS");
                    }
                    return true;
                })
                .orElseThrow(() -> new AccessDeniedException("NO_RELATIONSHIP_FOUND"));
    }

    public boolean canModifySchedule(Integer scheduleId, User user) {
        return medicationScheduleRepository.findById(scheduleId)
                .map(schedule -> {
                    if (schedule.getDoctor() == null || !schedule.getDoctor().getUserId().equals(user.getUserId())) {
                        throw new AccessDeniedException("ONLY_THE_PRESCRIBING_DOCTOR_CAN_MODIFY_THIS_SCHEDULE");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND"));
    }
}
