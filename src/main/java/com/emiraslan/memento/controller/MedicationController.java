package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.MedicationLogDto;
import com.emiraslan.memento.dto.MedicationScheduleDto;
import com.emiraslan.memento.service.MedicationLogService;
import com.emiraslan.memento.service.MedicationScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationScheduleService scheduleService;
    private final MedicationLogService logService;

    // SCHEDULE OPERATIONS

    // brings a patient's active schedules with their times
    @GetMapping("/schedules/patient/{patientId}")
    public ResponseEntity<List<MedicationScheduleDto>> getActiveSchedules(@PathVariable Integer patientId) {
        return ResponseEntity.ok(scheduleService.getActiveSchedulesByPatient(patientId));
    }

    // creating a new schedule
    @PostMapping("/schedules")
    public ResponseEntity<MedicationScheduleDto> createSchedule(@RequestBody MedicationScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.createSchedule(dto));
    }

    // update a schedule (see service for limitations)
    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<MedicationScheduleDto> updateSchedule(
            @PathVariable Integer scheduleId,
            @RequestBody MedicationScheduleDto dto
    ) {
        return ResponseEntity.ok(scheduleService.updateSchedule(scheduleId, dto));
    }

    // deactivate schedules
    @PatchMapping("/schedules/{scheduleId}/deactivate")
    public ResponseEntity<Void> deactivateSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deactivateSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    // LOG OPERATIONS

    // brings logs from a specific date
    // e.g. /api/v1/medications/logs?patientId=5&date=2023-11-28
    @GetMapping("/logs")
    public ResponseEntity<List<MedicationLogDto>> getLogs(
            @RequestParam Integer patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // if no date is given, today is returned by default
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(logService.getLogsByDate(patientId, targetDate));
    }

    // marks a medication as TAKEN
    @PostMapping("/logs/{timeId}/take")
    public ResponseEntity<MedicationLogDto> takeMedication(
            @PathVariable Integer timeId,
            @RequestParam Integer patientId
    ) {
        return ResponseEntity.ok(logService.logMedicationTaken(patientId, timeId));
    }

    // manually marks a medication as SKIPPED, in case the patient does not want to consume it
    @PostMapping("/logs/{timeId}/skip")
    public ResponseEntity<MedicationLogDto> skipMedication(
            @PathVariable Integer timeId,
            @RequestParam Integer patientId
    ) {
        return ResponseEntity.ok(logService.logMedicationSkipped(patientId, timeId));
    }
}