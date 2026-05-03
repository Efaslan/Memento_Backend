package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.response.MedicationLogResponseDto;
import com.emiraslan.memento.dto.response.MedicationLogSummaryResponseDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.MedicationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medications/logs")
@RequiredArgsConstructor
@Tag(name = "09 - Medication Logs")
@SecurityRequirement(name = "bearerAuth")
public class MedicationLogController {

    private final MedicationLogService logService;

    @Operation(
            summary = "Get my recent medication logs with statistics. Paginated.",
            description = "This week's logs will be returned on default."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me/recent")
    public ResponseEntity<MedicationLogSummaryResponseDto> getMyRecentLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "7") @Range(min = 0, max = 90) Integer daysBack,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Range(min = 1, max = 50) Integer size
    ) {
        return ResponseEntity.ok(logService.getRecentLogsSummary(user.getUserId(), daysBack, page, size));
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

    @Operation(
            summary = "Get patient's recent medication logs with statistics. Paginated.",
            description = "This week's logs will be returned on default."
    )
    @PreAuthorize("hasAnyAuthority('RELATIVE', 'DOCTOR') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/patient/{patientId}/recent")
    public ResponseEntity<MedicationLogSummaryResponseDto> getPatientsRecentLogs(
            @PathVariable Integer patientId,
            @RequestParam(defaultValue = "7") @Range(min = 0, max = 90) Integer daysBack,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Range(min = 0, max = 50) Integer size
    ) {
        return ResponseEntity.ok(logService.getRecentLogsSummary(patientId, daysBack, page, size));
    }
}
