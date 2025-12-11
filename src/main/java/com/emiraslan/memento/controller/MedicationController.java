package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.MedicationLogDto;
import com.emiraslan.memento.dto.MedicationScheduleDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.MedicationLogService;
import com.emiraslan.memento.service.MedicationScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
@Tag(name = "08 - Medications")
@SecurityRequirement(name = "bearerAuth")
public class MedicationController {

    private final MedicationScheduleService scheduleService;
    private final MedicationLogService logService;

    // PATIENT SCHEDULE OPERATIONS

    // brings a patient's active schedules with their times
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/schedules/me")
    public ResponseEntity<List<MedicationScheduleDto>> getMyActiveSchedules(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(scheduleService.getActiveSchedulesByPatient(user.getUserId()));
    }

    @Operation(
            summary = "Patient's entire medical history."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/schedules/me/history")
    public ResponseEntity<List<MedicationScheduleDto>> getMyScheduleHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(scheduleService.getAllSchedulesByPatient(user.getUserId()));
    }

    @Operation(
            summary = "Medications a patient takes on need without a specific time."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/schedules/me/prn")
    public ResponseEntity<List<MedicationScheduleDto>> getMyPrnSchedules(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(scheduleService.getPrnSchedulesByPatient(user.getUserId()));
    }

    //----------- PATIENT LOG OPERATIONS--------------

    @Operation(
            summary = "Patient's medication logs of a specific date.",
            description = "Today's logs will be returned if no date is given."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/logs/me")
    public ResponseEntity<List<MedicationLogDto>> getMyLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(logService.getLogsByDate(user.getUserId(), targetDate));
    }

    @Operation(
            summary = "Medication is logged as taken.",
            description = "Log status will only be 'taken' if the patient takes it within a 30 minute range. Past 30 minutes, log status will be 'delayed'."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @PostMapping("/logs/{timeId}/take")
    public ResponseEntity<MedicationLogDto> takeMedication(
            @PathVariable Integer timeId,
            @AuthenticationPrincipal User user
    ) {
        // we find the user through the token, so that others can not send post requests for a patient
        return ResponseEntity.ok(logService.logMedicationTaken(user.getUserId(), timeId));
    }

    // -----------------DOCTOR OPERATIONS-------------------
    @Operation(
            summary = "Medications I (a doctor) prescribed."
    )
    @PreAuthorize("hasAuthority('DOCTOR')")
    @GetMapping("/schedules/doctor/me")
    public ResponseEntity<List<MedicationScheduleDto>> getMyPrescriptions(@AuthenticationPrincipal User doctorUser) {
        return ResponseEntity.ok(scheduleService.getSchedulesByDoctor(doctorUser.getUserId()));
    }

    @PreAuthorize("hasAuthority('DOCTOR')")
    @PostMapping("/schedules")
    public ResponseEntity<MedicationScheduleDto> createSchedule(
            @RequestBody MedicationScheduleDto dto,
            @AuthenticationPrincipal User doctorUser
    ) {
        // we force the doctor's id from jwt instead of taking it from the dto
        dto.setDoctorUserId(doctorUser.getUserId());

        return ResponseEntity.ok(scheduleService.createSchedule(dto));
    }

    @Operation(
            summary = "Update an existing schedule. Please see descriptions.",
            description = "Updating a schedule is heavily restricted in order to protect a patient's medical history. Schedules can only be updated if the patient has no consumption logs on that schedule. If there are logs, only non-critical fields such as: Notes, End Date, and deactivation can be changed."
    )
    @PreAuthorize("hasAuthority('DOCTOR')")
    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<MedicationScheduleDto> updateSchedule(
            @PathVariable Integer scheduleId,
            @RequestBody MedicationScheduleDto dto
    ) {
        return ResponseEntity.ok(scheduleService.updateSchedule(scheduleId, dto));
    }

    @Operation(
            summary = "Deactivating schedules instead of deleting.",
            description = "This is in order to protect medical history. Patient's will not receive notifications about their deactivated schedules."
    )
    @PreAuthorize("hasAuthority('DOCTOR')")
    @PatchMapping("/schedules/{scheduleId}/deactivate")
    public ResponseEntity<Void> deactivateSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deactivateSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "A patient's active schedules for a doctor."
    )
    @PreAuthorize("hasAuthority('DOCTOR')")
    @GetMapping("/schedules/patient/{patientId}")
    public ResponseEntity<List<MedicationScheduleDto>> getPatientActiveSchedules(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(scheduleService.getActiveSchedulesByPatient(patientId));
    }

    @Operation(
            summary = "A patient's entire medical history for a doctor."
    )
    @PreAuthorize("hasAuthority('DOCTOR')")
    @GetMapping("/schedules/patient/{patientId}/history")
    public ResponseEntity<List<MedicationScheduleDto>> getPatientScheduleHistory(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(scheduleService.getAllSchedulesByPatient(patientId));
    }
}