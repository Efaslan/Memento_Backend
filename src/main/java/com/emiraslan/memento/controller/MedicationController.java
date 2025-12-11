package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.MedicationLogDto;
import com.emiraslan.memento.dto.MedicationScheduleDto;
import com.emiraslan.memento.service.MedicationLogService;
import com.emiraslan.memento.service.MedicationScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
@Tag(name = "08 - Medications")
public class MedicationController {

    private final MedicationScheduleService scheduleService;
    private final MedicationLogService logService;

    // SCHEDULE OPERATIONS

    // brings a patient's active schedules with their times
    @GetMapping("/schedules/patient/{patientId}")
    public ResponseEntity<List<MedicationScheduleDto>> getActiveSchedules(@PathVariable Integer patientId) {
        return ResponseEntity.ok(scheduleService.getActiveSchedulesByPatient(patientId));
    }

    @Operation(
            summary = "Patient's entire medical history."
    )
    @GetMapping("/schedules/patient/{patientId}/history")
    public ResponseEntity<List<MedicationScheduleDto>> getScheduleHistory(@PathVariable Integer patientId) {
        return ResponseEntity.ok(scheduleService.getAllSchedulesByPatient(patientId));
    }

    @Operation(
            summary = "Medications a patient takes on need without a specific time."
    )
    @GetMapping("/schedules/patient/{patientId}/prn")
    public ResponseEntity<List<MedicationScheduleDto>> getPrnSchedules(@PathVariable Integer patientId) {
        return ResponseEntity.ok(scheduleService.getPrnSchedulesByPatient(patientId));
    }

    @Operation(
            summary = "Medications prescribed by a specific doctor."
    )
    @GetMapping("/schedules/doctor/{doctorId}")
    public ResponseEntity<List<MedicationScheduleDto>> getDoctorSchedules(@PathVariable Integer doctorId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByDoctor(doctorId));
    }

    // creating a new schedule
    @PostMapping("/schedules")
    public ResponseEntity<MedicationScheduleDto> createSchedule(@RequestBody MedicationScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.createSchedule(dto));
    }

    @Operation(
            summary = "Update an existing schedule. Please see descriptions.",
            description = "Updating a schedule is heavily restricted in order to protect a patient's medical history. Schedules can only be updated if the patient has no consumption logs on that schedule. If there are logs, only non-critical fields such as: Notes, End Date, and deactivation can be changed."
    )
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
    @PatchMapping("/schedules/{scheduleId}/deactivate")
    public ResponseEntity<Void> deactivateSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deactivateSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    // LOG OPERATIONS

    @Operation(
            summary = "Patient's medication logs of a specific date.",
            description = "Today's logs will be returned if no date is given."
    )
    @GetMapping("/logs")
    public ResponseEntity<List<MedicationLogDto>> getLogs(
            @RequestParam Integer patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(logService.getLogsByDate(patientId, targetDate));
    }

    // marks a medication as TAKEN
    @Operation(
            summary = "Medication is logged as taken.",
            description = "Log status will only be 'taken' if the patient takes it within a 30 minute range. Past 30 minutes, log status will be 'delayed'."
    )
    @PostMapping("/logs/{timeId}/take")
    public ResponseEntity<MedicationLogDto> takeMedication(
            @PathVariable Integer timeId,
            @RequestParam Integer patientId
    ) {
        return ResponseEntity.ok(logService.logMedicationTaken(patientId, timeId));
    }

    @Operation(
            summary = "Medication is logged as skipped.",
            description = "Medication that a patient hasn't taken for 2 hours will be logged automatically as 'skipped'."
    )
    @PostMapping("/logs/{timeId}/skip")
    public ResponseEntity<MedicationLogDto> skipMedication(
            @PathVariable Integer timeId,
            @RequestParam Integer patientId
    ) {
        return ResponseEntity.ok(logService.logMedicationSkipped(patientId, timeId));
    }
}