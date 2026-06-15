package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.response.MedicationLogResponseDto;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.service.medication.MedicationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medications/logs")
@RequiredArgsConstructor
@Tag(name = "12 - Medication Logs")
@SecurityRequirement(name = "bearerAuth")
public class MedicationLogController {

    private final MedicationLogService logService;

    @Operation(
            summary = "Brings a medication schedule's logs. Maximum daysBack is 14."
    )
    @GetMapping("/{scheduleId}")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE', 'DOCTOR') and @guard.canAccessSchedule(#scheduleId, principal)")
    public ResponseEntity<List<MedicationLogResponseDto>> getScheduleLogs(
            @PathVariable Integer scheduleId,
            @RequestParam(required = false, defaultValue = "7") @Range(min = 7, max = 14) Integer daysBack
    ) {
        List<MedicationLogResponseDto> response = logService.getLogsBySchedule(scheduleId, daysBack);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Medication is logged as taken.",
            description = "Log status will only be 'taken' if the patient takes it within a 30 minute range. Past 30 minutes, log status will be 'delayed'."
    )
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isScheduleTimeOwner(#timeId, principal)")
    @PostMapping("/{timeId}/take")
    public ResponseEntity<MedicationLogResponseDto> takeMedication(
            @PathVariable Integer timeId,
            @AuthenticationPrincipal User patient
    ) {
        return ResponseEntity.ok(logService.logMedicationTaken(patient, timeId));
    }
}