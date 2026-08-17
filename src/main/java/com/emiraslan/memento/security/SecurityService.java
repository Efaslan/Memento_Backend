package com.emiraslan.memento.security;

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
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;

    // Checking for an active relationship for relatives to view PATIENT data
    public boolean isThePatientOrTheirRelative(Integer targetPatientId, User user) {

        if (user.getRole() == UserRole.PATIENT) {
            if (!targetPatientId.equals(user.getUserId())) {
                throw new AccessDeniedException("PATIENTS_CAN_ONLY_ACCESS_THEIR_OWN_DATA");
            }
            return true;
        }
        String redisKey = "relationship:" + targetPatientId + ":" + user.getUserId();
        String cachedValue = redisTemplate.opsForValue().get(redisKey);

        // Check for relationships between 2 users if not
        if (cachedValue != null) {
            return true; // if cachedValue exists in Redis, that means they have a relationship
        } else {
            throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
        }
    }

    // ========================================================================
    // RELATIONSHIP CHECKS for UPDATES
    // ========================================================================
    public boolean canModifyGoal(Integer goalId, User user) {

        Integer ownerPatientId = goalRepository.findPatientIdByGoalId(goalId)
                .orElseThrow(() -> new EntityNotFoundException("GOAL_NOT_FOUND"));

        return isThePatientOrTheirRelative(ownerPatientId, user);
    }

    public boolean canManageDevice(Integer deviceId, User user){
        UserDevice device = userDeviceRepository.findByIdWithUser(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_NOT_FOUND"));

        return isThePatientOrTheirRelative(device.getUser().getUserId(), user);
    }

    // Checking for INVOLVEMENT during these UPDATES
    // ========================================================================
    public boolean canModifyRelationship(Integer relationshipId, User user) {
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

    public boolean canModifySchedule(Integer scheduleId, User user) {
        return medicationScheduleRepository.findById(scheduleId)
                .map(schedule -> {
                    boolean isCreator = schedule.getCreator() != null && Objects.equals(schedule.getCreator().getUserId(), user.getUserId());
                    boolean isPatient = Objects.equals(schedule.getPatient().getUserId(), user.getUserId());

                    if (!isCreator && !isPatient) {
                        throw new AccessDeniedException("YOU_ARE_NOT_PART_OF_THIS_SCHEDULE");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND"));
    }

    // ========================================================================
    // OWNERSHIP CHECKS
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

    public boolean isScheduleTimeOwner(Integer timeId, User user){
        return timesRepository.findById(timeId)
                .map(time -> {
                    if(!time.getSchedule().getPatient().getUserId().equals(user.getUserId())){
                        throw new AccessDeniedException("NOT_YOUR_MEDICATION");
                    }
                    return true;
                }).orElseThrow(() -> new EntityNotFoundException("SCHEDULE_TIME_NOT_FOUND"));
    }

    public boolean isDeviceOwner(Integer deviceId, User user){
        UserDevice device = userDeviceRepository.findByIdWithUser(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_NOT_FOUND"));

        if(!device.getUser().getUserId().equals(user.getUserId())){
            throw new AccessDeniedException("YOU_ARE_NOT_DEVICE_OWNER");
        }
        return true;
    }

    public boolean isGoalOwner(Integer goalId, User user) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("GOAL_NOT_FOUND"));

        if (!goal.getPatient().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("ONLY_GOAL_OWNERS_CAN_COMPLETE_THEIR_GOALS");
        }
        return true;
    }

    // ========================================================================
    // OTHER CHECKS (Alerts are currently unused)
    // ========================================================================

    public boolean canAccessSchedule(Integer scheduleId, User user) {

        Integer ownerPatientId = medicationScheduleRepository.findPatientIdByScheduleId(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND"));

        return isThePatientOrTheirRelative(ownerPatientId, user);
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
}
