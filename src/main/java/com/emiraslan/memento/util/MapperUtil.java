package com.emiraslan.memento.util;

import com.emiraslan.memento.dto.*;
import com.emiraslan.memento.dto.request.*;
import com.emiraslan.memento.dto.response.*;
import com.emiraslan.memento.dto.response.medication.MedicationLogResponseDto;
import com.emiraslan.memento.dto.response.medication.MedicationScheduleResponseDto;
import com.emiraslan.memento.entity.*;
import com.emiraslan.memento.entity.medication.MedicationLog;
import com.emiraslan.memento.entity.medication.MedicationSchedule;
import com.emiraslan.memento.entity.medication.MedicationScheduleTime;
import com.emiraslan.memento.entity.user.*;
import com.emiraslan.memento.enums.ConsentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class MapperUtil {

    // User Mapping
    public static UserResponseDto toUserResponseDto(User user) {
        if (user == null) return null;
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .role(user.getRole())
                .build();
    } // see AuthService for dto->entity

    // SavedLocation Mapping
    public static SavedLocationResponseDto toSavedLocationResponseDto(SavedLocation entity) {
        if (entity == null) return null;
        return SavedLocationResponseDto.builder()
                .locationId(entity.getLocationId())
                .patientUserId(entity.getPatient().getUserId()) // Dto(and mobile) only needs the ID
                .locationName(entity.getLocationName())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .addressDetails(entity.getAddressDetails())
                .build();
    }

    public static SavedLocation toSavedLocationEntity(SavedLocationRequestDto dto, User patient) {
        if (dto == null) return null;
        return SavedLocation.builder()
                .patient(patient) // JPA accepts the object as FK, not just ID(see entity package FKs)
                .locationName(dto.getLocationName())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .addressDetails(dto.getAddressDetails())
                .build();
    }

    // Alert Mapping
    public static AlertResponseDto toAlertResponseDto(Alert entity) {
        if (entity == null) return null;

        Integer ackUserId = null;
        String ackUserName = null;

        if (entity.getAcknowledgedBy() != null) {
            ackUserId = entity.getAcknowledgedBy().getUserId();
            ackUserName = entity.getAcknowledgedBy().getFirstName() + " " + entity.getAcknowledgedBy().getLastName();
        }

        return AlertResponseDto.builder()
                .alertId(entity.getAlertId())
                .patientUserId(entity.getPatient().getUserId())
                .alertType(entity.getAlertType())
                .alertTimestamp(entity.getAlertTimestamp())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .status(entity.getStatus())
                .details(entity.getDetails())
                .acknowledgedByUserId(ackUserId)
                .acknowledgedByName(ackUserName)
                .build();
    }

    public static Alert toAlertEntity(AlertRequestDto dto, User patient) {
        if (dto == null) return null;
        return Alert.builder()
                .patient(patient)
                .alertType(dto.getAlertType())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                // Builder.default sets Status and Timestamp, service might @Override
                .build();
    }

    // GeneralReminder Mapping
    public static GeneralReminderResponseDto toGeneralReminderResponseDto(GeneralReminder entity) {
        if (entity == null) return null;

        String creatorName = "System"; // default in case creator is null
        Integer creatorId = null;

        if (entity.getCreator() != null) {
            creatorId = entity.getCreator().getUserId();
            creatorName = entity.getCreator().getFirstName() + " " + entity.getCreator().getLastName();
        }

        return GeneralReminderResponseDto.builder()
                .reminderId(entity.getReminderId())
                .patientUserId(entity.getPatient().getUserId())
                .creatorUserId(creatorId)
                .creatorName(creatorName) // creator's entire name for display
                .title(entity.getTitle())
                .reminderTime(entity.getReminderTime())
                .isRecurring(entity.getIsRecurring())
                .recurrenceRule(entity.getRecurrenceRule())
                .build();
    }

    public static GeneralReminder toGeneralReminderEntity(GeneralReminderRequestDto dto, User patient, User creator) {
        if (dto == null) return null;
        return GeneralReminder.builder()
                .patient(patient)
                .creator(creator) // can be null, or equal to patient
                .title(dto.getTitle())
                .reminderTime(dto.getReminderTime())
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .recurrenceRule(dto.getRecurrenceRule())
                .build();
    }

    // PatientRelationship Mapping
    public static RelationshipResponseDto toRelationshipResponseDto(PatientRelationship entity) {
        if (entity == null) return null;

        User caregiver = entity.getCaregiver(); // caregiver(relative) cannot be null

        return RelationshipResponseDto.builder()
                .relationshipId(entity.getRelationshipId())
                .patientUserId(entity.getPatient().getUserId())
                .patientName(entity.getPatient().getFirstName() + " " + entity.getPatient().getLastName())
                .caregiverUserId(caregiver.getUserId())
                .caregiverName(caregiver.getFirstName() + " " + caregiver.getLastName()) // for display
                .caregiverPhone(caregiver.getPhoneNumber())
                .caregiverEmail(caregiver.getEmail())
                .relationshipType(entity.getRelationshipType())
                .isPrimaryContact(entity.getIsPrimaryContact())
                .isActive(entity.getIsActive())
                .build();
    }

    // DailyLog Mapping
    public static DailyLogResponseDto toDailyLogResponseDto(DailyLog entity) {
        if (entity == null) return null;
        return DailyLogResponseDto.builder()
                .dailyLogId(entity.getDailyLogId())
                .patientUserId(entity.getPatient().getUserId())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static DailyLog toDailyLogEntity(DailyLogRequestDto dto, User patient) {
        if (dto == null) return null;
        return DailyLog.builder()
                .patient(patient)
                .description(dto.getDescription())
                // createdAt is now() by default
                .build();
    }

    // MedicationSchedule Mapping, combines schedule with its times into method
    public static MedicationScheduleResponseDto toMedicationScheduleResponseDto(MedicationSchedule entity, List<MedicationScheduleTime> times) {
        if (entity == null) return null;

        String creatorName = "Unknown"; // default in case of null
        Integer creatorId = null;

        if (entity.getCreator() != null) {
            creatorId = entity.getCreator().getUserId();
            creatorName = entity.getCreator().getFirstName() + " " + entity.getCreator().getLastName();
        }

        List<MedicationScheduleResponseDto.TimeInfoDto> timeList =
                times.stream()
                        .map(timeEntity -> new MedicationScheduleResponseDto.TimeInfoDto(
                                timeEntity.getTimeId(),
                                timeEntity.getScheduledTime()
                        ))
                        .collect(Collectors.toList());

        return MedicationScheduleResponseDto.builder()
                .scheduleId(entity.getScheduleId())
                .patientUserId(entity.getPatient().getUserId())
                .creatorUserId(creatorId)
                .creatorName(creatorName)
                .medicationName(entity.getMedicationName())
                .dosage(entity.getDosage())
                .notes(entity.getNotes())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isPrn(entity.getIsPrn())
                .isActive(entity.getIsActive())
                .times(timeList) // created a LocalTime list with ids
                .build();
    }

    // No ScheduleTimes here, Service saves them through a loop
    public static MedicationSchedule toMedicationScheduleEntity(MedicationScheduleRequestDto dto, User patient, User creator) {
        if (dto == null) return null;
        return MedicationSchedule.builder()
                .patient(patient)
                .creator(creator) // can be null
                .medicationName(dto.getMedicationName())
                .dosage(dto.getDosage())
                .notes(dto.getNotes())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .isPrn(dto.getIsPrn() != null ? dto.getIsPrn() : false)
                .build();
    }

    // MedicationLog Mapping (entity->dto only, service handles dto->entity)
    public static MedicationLogResponseDto toMedicationLogResponseDto(MedicationLog entity) {
        if (entity == null) return null;

        return MedicationLogResponseDto.builder()
                .medicationLogId(entity.getMedicationLogId())
                .scheduleTimeId(entity.getScheduleTime().getTimeId()) // medicine's assigned time id
                .takenAt(entity.getTakenAt())
                .status(entity.getStatus())
                .build();
    }

    // PatientProfile Mapping (entity->dto only)
    public static PatientProfileResponseDto toPatientProfileResponseDto(PatientProfile entity) {
        if (entity == null) return null;

        User patient = entity.getPatient();

        return PatientProfileResponseDto.builder()
                .patientUserId(entity.getPatientUserId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .email(patient.getEmail())
                .phoneNumber(patient.getPhoneNumber())
                .dateOfBirth(entity.getDateOfBirth())
                .heightCm(entity.getHeightCm())
                .weightKg(entity.getWeightKg())
                .bloodType(entity.getBloodType())
                .emergencyNotes(entity.getEmergencyNotes())
                .build();
    }

    // for device and tokens
    public static UserDevice toUserDeviceEntity(User user, String deviceModel, String osVersion) {
        return UserDevice.builder()
                .user(user)
                .deviceModel(deviceModel)
                .osVersion(osVersion)
                .lastActive(LocalDateTime.now())
                .build();
    }

    public static RefreshToken toRefreshTokenEntity(UserDevice device, String hashedJti) {
        return RefreshToken.builder()
                .userDevice(device)
                .refreshToken(hashedJti)
                .expiryDate(LocalDateTime.now().plusDays(14))
                .build();
    }

    public static UserDeviceResponseDto toUserDeviceResponseDto(UserDevice device) {
        if (device == null) {
            return null;
        }

        return UserDeviceResponseDto.builder()
                .deviceId(device.getDeviceId())
                .deviceModel(device.getDeviceModel())
                .osVersion(device.getOsVersion())
                .biometricEnabled(device.getBiometricEnabled())
                .lastActive(device.getLastActive())
                .build();
    }

    // For User Consents
    public static UserConsent toUserConsentEntity(User user, ConsentType consentType, String documentVersion, Boolean isAccepted, String ipAddress, String userAgent) {
        return UserConsent.builder()
                .user(user)
                .consentType(consentType)
                .documentVersion(documentVersion)
                .isAccepted(isAccepted)
                .consentedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }

    // For Goals
    public static GoalResponseDto toGoalResponseDto(Goal goal) {
        if (goal == null) {
            return null;
        }

        return GoalResponseDto.builder()
                .goalId(goal.getGoalId())
                .goalType(goal.getGoalType())
                .title(goal.getTitle())
                .currentTargetValue(goal.getCurrentTargetValue())
                .unit(goal.getUnit())
                .isActive(goal.getIsActive())
                .createdAt(goal.getCreatedAt())
                .build();
    }

    public static Goal toGoalEntity(GoalRequestDto dto, User patient, User creator){
        return Goal.builder()
                .patient(patient)
                .creator(creator)
                .goalType(dto.getGoalType())
                .title(dto.getTitle())
                .currentTargetValue(dto.getCurrentTargetValue())
                .unit(dto.getUnit())
                .isActive(true)
                .build();
    }

    // For Goal Logs
    public static GoalLog toGoalLogEntity(GoalLogRequestDto dto, Goal goal) {
        if (dto == null || goal == null) {
            return null;
        }

        return GoalLog.builder()
                .goal(goal)
                .progressValue(dto.getProgressValue())
                .logTargetValue(goal.getCurrentTargetValue())
                .logUnit(goal.getUnit())
                // createdAt is automatic
                .build();
    }

    public static GoalLogResponseDto toGoalLogResponseDto(GoalLog log) {
        if (log == null) {
            return null;
        }

        return GoalLogResponseDto.builder()
                .goalLogId(log.getGoalLogId())
                .status(log.getStatus())
                .progressValue(log.getProgressValue())
                .logTargetValue(log.getLogTargetValue())
                .logUnit(log.getLogUnit())
                .createdAt(log.getCreatedAt())
                .build();
    }
}