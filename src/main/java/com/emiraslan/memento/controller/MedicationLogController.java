package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.MedicationLogDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.MedicationLogService;
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
@RequestMapping("/api/v1/medications/logs")
@RequiredArgsConstructor
@Tag(name = "09 - Medication Logs")
@SecurityRequirement(name = "bearerAuth")
public class MedicationLogController {

    private final MedicationLogService logService;

    @Operation(
            summary = "Patient's medication logs of a specific date.",
            description = "Today's logs will be returned if no date is given."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me/date")
    public ResponseEntity<List<MedicationLogDto>> getMyLogsByDate(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(logService.getLogsByDate(user.getUserId(), targetDate));
    }

    @Operation(
            summary = "All of a patient's medication logs."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me")
    public ResponseEntity<List<MedicationLogDto>> getAllOfMyLogs(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(logService.getAllLogs(user.getUserId()));
    }

    @Operation(
            summary = "Medication is logged as taken.",
            description = "Log status will only be 'taken' if the patient takes it within a 30 minute range. Past 30 minutes, log status will be 'delayed'."
    )
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isScheduleTimeOwner(#timeId, principal)")
    @PostMapping("/{timeId}/take")
    public ResponseEntity<MedicationLogDto> takeMedication(
            @PathVariable Integer timeId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(logService.logMedicationTaken(user.getUserId(), timeId));
    }

    @Operation(
            summary = "All of a patient's medication logs for relatives and doctors."
    )
    @PreAuthorize("hasAnyAuthority('RELATIVE', 'DOCTOR') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/{patientId}")
    public ResponseEntity<List<MedicationLogDto>> getAllPatientLogs(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(logService.getAllLogs(patientId));
    }

    @Operation(
            summary = "Patient's medication logs of a specific date for relatives and doctors.",
            description = "Today's logs will be returned if no date is given."
    )
    @PreAuthorize("hasAnyAuthority('RELATIVE', 'DOCTOR') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/{patientId}/date")
    public ResponseEntity<List<MedicationLogDto>> getPatientLogsByDate(
            @PathVariable Integer patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(logService.getLogsByDate(patientId, targetDate));
    }
}
