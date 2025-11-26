package com.emiraslan.memento.util;

import com.emiraslan.memento.dto.*;
import com.emiraslan.memento.entity.*;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
// only entity -> dto conversions
public class MapperUtil {

    // User Mapping
    public static UserDto toUserDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .build();
    }

    // SavedLocation Mapping
    public static SavedLocationDto toSavedLocationDto(SavedLocation entity) {
        if (entity == null) return null;
        return SavedLocationDto.builder()
                .locationId(entity.getLocationId())
                .patientUserId(entity.getPatient().getUserId()) // Dto(and mobile) only needs the ID
                .locationName(entity.getLocationName())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .addressDetails(entity.getAddressDetails())
                .build();
    }

//    public static SavedLocation toSavedLocationEntity(SavedLocationDto dto, User patient) {
//        if (dto == null) return null;
//        return SavedLocation.builder()
//                .locationId(dto.getLocationId())
//                .patient(patient) // JPA accepts the object as FK, not just ID(see entity package FKs)
//                .locationName(dto.getLocationName())
//                .latitude(dto.getLatitude())
//                .longitude(dto.getLongitude())
//                .addressDetails(dto.getAddressDetails())
//                .build();
//    }

    // Alert Mapping
    public static AlertDto toAlertDto(Alert entity) {
        if (entity == null) return null;
        return AlertDto.builder()
                .alertId(entity.getAlertId())
                .patientUserId(entity.getPatient().getUserId())
                .alertType(entity.getAlertType())
                .alertTimestamp(entity.getAlertTimestamp())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .status(entity.getStatus())
                .details(entity.getDetails())
                .build();
    }

    // GeneralReminder Mapping
    public static GeneralReminderDto toGeneralReminderDto(GeneralReminder entity) {
        if (entity == null) return null;

        String creatorName = "Sistem"; // default in case creator is null
        Integer creatorId = null;

        if (entity.getCreator() != null) {
            creatorId = entity.getCreator().getUserId();
            creatorName = entity.getCreator().getFirstName() + " " + entity.getCreator().getLastName();
        }

        return GeneralReminderDto.builder()
                .reminderId(entity.getReminderId())
                .patientUserId(entity.getPatient().getUserId())
                .creatorUserId(creatorId)
                .creatorName(creatorName) // creator's entire name for display
                .title(entity.getTitle())
                .reminderTime(entity.getReminderTime())
                .isRecurring(entity.getIsRecurring())
                .recurrenceRule(entity.getRecurrenceRule())
                .isCompleted(entity.getIsCompleted())
                .build();
    }

    // PatientRelationship Mapping
    public static PatientRelationshipDto toPatientRelationshipDto(PatientRelationship entity) {
        if (entity == null) return null;

        User caregiver = entity.getCaregiver(); // caregiver(relative or doctor) cannot be null

        return PatientRelationshipDto.builder()
                .relationshipId(entity.getRelationshipId())
                .patientUserId(entity.getPatient().getUserId())
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
    public static DailyLogDto toDailyLogDto(DailyLog entity) {
        if (entity == null) return null;
        return DailyLogDto.builder()
                .dailyLogId(entity.getDailyLogId())
                .patientUserId(entity.getPatient().getUserId())
                .logType(entity.getLogType())
                .description(entity.getDescription())
                .quantityMl(entity.getQuantityMl())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // MedicationSchedule Mapping, combines schedule with its times into method
    public static MedicationScheduleDto toMedicationScheduleDto(MedicationSchedule entity, List<MedicationScheduleTime> times) {
        if (entity == null) return null;

        String doctorName = "Bilinmiyor"; // default in case of null
        Integer doctorId = null;

        if (entity.getDoctor() != null) {
            doctorId = entity.getDoctor().getUserId();
            doctorName = entity.getDoctor().getFirstName() + " " + entity.getDoctor().getLastName();
        }

        // create a LocalTime list from times
        List<LocalTime> timeList = (times != null) ?
                times.stream()
                        .map(MedicationScheduleTime::getScheduledTime)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return MedicationScheduleDto.builder()
                .scheduleId(entity.getScheduleId())
                .patientUserId(entity.getPatient().getUserId())
                .doctorUserId(doctorId)
                .doctorName(doctorName)
                .medicationName(entity.getMedicationName())
                .dosage(entity.getDosage())
                .notes(entity.getNotes())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isPrn(entity.getIsPrn())
                .isActive(entity.getIsActive())
                .times(timeList) // created LocalTime list
                .build();
    }

    // MedicationLog Mapping
    public static MedicationLogDto toMedicationLogDto(MedicationLog entity) {
        if (entity == null) return null;

        // get medicationName through relations
        String medicationName = "";
        if (entity.getScheduleTime() != null && entity.getScheduleTime().getSchedule() != null) {
            medicationName = entity.getScheduleTime().getSchedule().getMedicationName();
        }

        return MedicationLogDto.builder()
                .medicationLogId(entity.getMedicationLogId())
                // Sadece Time ID'si yerine Schedule ID'si de gerekebilir ama şimdilik time yeterli
                .scheduleTimeId(entity.getScheduleTime().getTimeId())
                .patientUserId(entity.getPatient().getUserId())
                .takenAt(entity.getTakenAt())
                .status(entity.getStatus())
                .medicationName(medicationName) // for display
                .build();
    }

    // DoctorProfile Mapping
    public static DoctorProfileDto toDoctorProfileDto(DoctorProfile entity) {
        if (entity == null) return null;

        User doctor = entity.getDoctor();

        return DoctorProfileDto.builder()
                .doctorUserId(entity.getDoctorUserId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .email(doctor.getEmail())
                .specialization(entity.getSpecialization())
                .hospitalName(entity.getHospitalName())
                .title(entity.getTitle())
                .build();
    }

    // PatientProfile Mapping
    public static PatientProfileDto toPatientProfileDto(PatientProfile entity) {
        if (entity == null) return null;

        User patient = entity.getPatient();

        return PatientProfileDto.builder()
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
}