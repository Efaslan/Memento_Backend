package com.emiraslan.memento.security;

import com.emiraslan.memento.dto.request.GeneralReminderRequestDto;
import com.emiraslan.memento.dto.request.GoalRequestDto;
import com.emiraslan.memento.dto.request.MedicationScheduleRequestDto;
import com.emiraslan.memento.entity.Goal;
import com.emiraslan.memento.entity.UserDevice;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.*;
import com.emiraslan.memento.repository.device.UserDeviceRepository;
import com.emiraslan.memento.repository.medication.MedicationScheduleRepository;
import com.emiraslan.memento.repository.medication.MedicationScheduleTimeRepository;
import com.emiraslan.memento.repository.user.PatientRelationshipRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
    private final UserDeviceRepository userDeviceRepository;
    private final GoalRepository goalRepository;

    // --- helper method ----
    private boolean hasActiveRelationship(Integer patientId, Integer caregiverId){
        return relationshipRepository.findByPatient_UserIdAndCaregiver_UserId(patientId, caregiverId)
                .map(rel -> Boolean.TRUE.equals(rel.getIsActive()))
                .orElse(false);
    }

    // todo mobilden istek gelirken her zaman patientId bilinicek cunku onu secip ekranina giriyoruz. Bu yuzden relative endpointlerinin hepsinde patientId ekleyebiliriz, her birine ayri security metodu eklemek yerine, sadece iliski kontrolu yapip geceriz. Relative endpointleri icin
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

    public boolean canCreateReminder(GeneralReminderRequestDto dto, User user) {
        // patients do not need to include id in their request. It is automatically set in service
        if (user.getRole() == UserRole.PATIENT) {
            return true;
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

    public boolean canCreateSchedule(MedicationScheduleRequestDto dto, User user){

        return relationshipRepository.findByPatient_UserIdAndCaregiver_UserId(dto.getPatientUserId(), user.getUserId())
                .map(rel -> {
                    if (!Boolean.TRUE.equals(rel.getIsActive())) {
                        throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
                    }
                    return true;
                })
                .orElseThrow(() -> new AccessDeniedException("NO_RELATIONSHIP_FOUND"));
    }

    public boolean canModifySchedule(Integer scheduleId, User user) {
        return medicationScheduleRepository.findById(scheduleId)
                .map(schedule -> {
                    if (!Objects.equals(schedule.getCreator().getUserId(), user.getUserId()) || !Objects.equals(user.getUserId(), schedule.getPatient().getUserId())) {
                        throw new AccessDeniedException("YOU_ARE_NOT_PART_OF_THIS_SCHEDULE");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND"));
    }

    public boolean canAccessSchedule(Integer scheduleId, User user) {

        Integer targetPatientId = medicationScheduleRepository.findPatientIdByScheduleId(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND"));

        return canViewPatientData(targetPatientId, user);
    }

    // ========================================================================
    // USER DEVICE SECURITY
    // ========================================================================

    public boolean isDeviceOwner(Integer deviceId, User user){
        UserDevice device = userDeviceRepository.findByIdWithUser(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_NOT_FOUND"));

        if(!device.getUser().getUserId().equals(user.getUserId())){
            throw new AccessDeniedException("YOU_ARE_NOT_DEVICE_OWNER");
        }
        return true;
    }

    public boolean canManageDevice(Integer deviceId, User user){
        UserDevice device = userDeviceRepository.findByIdWithUser(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_NOT_FOUND"));

        User deviceOwner = device.getUser();

        // if device owner is the user making the request, allow them
        if (deviceOwner.getUserId().equals(user.getUserId())) {
            return true;
        }

        // if the user isn't the device owner, and isn't related to the owner, throw 403
        if(!hasActiveRelationship(deviceOwner.getUserId(), user.getUserId())){
            throw new AccessDeniedException("YOU_ARE_NOT_RELATED_TO_DEVICE_OWNER");
        }
        return true;
    }

    // ========================================================================
    // GOAL SECURITY
    // ========================================================================

    public boolean canCreateGoal(GoalRequestDto dto, User user) {
        if (user.getRole() == UserRole.PATIENT) {
            return true;
        }

        if (!hasActiveRelationship(dto.getPatientUserId(), user.getUserId())) {
            throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
        }
        return true;
    }

    public boolean canModifyGoal(Integer goalId, User user) {

        Integer targetPatientId = goalRepository.findPatientIdByGoalId(goalId)
                .orElseThrow(() -> new EntityNotFoundException("GOAL_NOT_FOUND"));

        return canViewPatientData(targetPatientId, user);
    }

    public boolean canCompleteGoal(Integer goalId, User user) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("GOAL_NOT_FOUND"));

        if (!goal.getPatient().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("ONLY_GOAL_OWNERS_CAN_COMPLETE_THEIR_GOALS");
        }
        return true;
    }
}
